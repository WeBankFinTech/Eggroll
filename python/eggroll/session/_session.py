#  Copyright (c) 2019 - now, Eggroll Authors. All Rights Reserved.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
import logging
import os
from concurrent.futures import wait, FIRST_EXCEPTION

from eggroll.config import Config, ConfigKey
from eggroll.core.command.command_client import ClusterManagerClient
from eggroll.core.command.command_status import SessionStatus
from eggroll.core.datastructure.threadpool import ErThreadUnpooledExecutor
from eggroll.core.meta_model import ErSessionMeta, ErPartition, ErStore
from ._utils import get_stack, time_now, get_self_ip

L = logging.getLogger(__name__)


def session_init(
    session_id, options: dict = None, config=None, config_properties_file=None
) -> "ErSession":
    if config is None:
        config = Config().load_default()
        if config_properties_file:
            config.load_properties(config_properties_file)
        elif eggroll_home := os.getenv("EGGROLL_HOME", None):
            path = f"{eggroll_home}/conf/eggroll.properties"
            if os.path.exists(path):
                config.load_properties(path)
        config.load_env()
    er_session = ErSession(config=config, session_id=session_id, options=options)
    return er_session


class ErSession(object):
    def __init__(
        self,
        config: Config,
        session_id=None,
        name="",
        tag="",
        processors: list = None,
        options: dict = None,
    ):
        self._config = config
        if processors is None:
            processors = []
        if options is None:
            options = {}
        if not session_id:
            self.__session_id = f"er_session_py_{time_now()}_{get_self_ip()}"
        else:
            self.__session_id = session_id

        if "EGGROLL_DEBUG" not in os.environ:
            os.environ["EGGROLL_DEBUG"] = "0"

        self.__options = options.copy()
        self.__options[ConfigKey.eggroll.session.id] = self.__session_id
        self._cluster_manager_client = ClusterManagerClient(config=config)
        session_meta = ErSessionMeta(
            id=self.__session_id,
            name=name,
            status=SessionStatus.NEW,
            tag=tag,
            processors=processors,
            options=options,
        )
        self.executor = ErThreadUnpooledExecutor(
            max_workers=config.eggroll.core.client.command.executor.pool.max.size,
            thread_name_prefix="session_server",
        )

        from time import monotonic, sleep

        timeout = config.eggroll.session.start.timeout.ms / 1000 + 2
        endtime = monotonic() + timeout

        # TODO:0: ignores exception while starting up in standalone mod
        while True:
            try:
                if not processors:
                    self.__session_meta = (
                        self._cluster_manager_client.get_or_create_session(session_meta)
                    )
                else:
                    self.__session_meta = self._cluster_manager_client.register_session(
                        session_meta
                    )
                break
            except Exception as e:
                print(e)
                if monotonic() < endtime:
                    sleep(0.1)
                else:
                    raise

        self.__exit_tasks = list()
        self.__processors = self.__session_meta.processors

        L.info(
            f"session init finished: {self.__session_id}, details: {self.__session_meta}"
        )
        self.stopped = (
            self.__session_meta.status == SessionStatus.CLOSED
            or self.__session_meta.status == SessionStatus.KILLED
        )
        self._rolls = list()
        self._eggs = dict()

        for processor in self.__session_meta.processors:
            if processor.is_egg_pair():
                server_node_id = processor.server_node_id
                if server_node_id not in self._eggs:
                    self._eggs[server_node_id] = list()
                self._eggs[server_node_id].append(processor)
            elif processor.is_roll_pair_master():
                self._rolls.append(processor)
            else:
                raise ValueError(
                    f"processor type {processor.processor_type} not supported in roll pair"
                )

    @property
    def eggs(self):
        return self._eggs

    @property
    def config(self):
        return self._config

    def is_active(self):
        return self.__session_meta.status == SessionStatus.ACTIVE

    @property
    def cluster_manager_client(self):
        return self._cluster_manager_client

    def get_rank_in_node(self, partition_id, server_node_id):
        processor_count_of_node = len(self._eggs[server_node_id])
        cluster_node_count = len(self._eggs)
        rank_in_node = _calculate_rank_in_node(
            partition_id, cluster_node_count, processor_count_of_node
        )

        return rank_in_node

    def route_to_egg(self, partition: ErPartition):
        server_node_id = partition.processor._server_node_id
        rank_in_node = partition._rank_in_node
        if partition._rank_in_node is None or rank_in_node < 0:
            rank_in_node = self.get_rank_in_node(
                partition_id=partition._id, server_node_id=server_node_id
            )

        result = self.route_to_egg_by_rank(server_node_id, rank_in_node)

        return result

    def route_to_egg_by_rank(self, server_node_id, rank_in_node):
        result = self._eggs[server_node_id][rank_in_node]
        if not result._command_endpoint._host or result._command_endpoint._port <= 0:
            raise ValueError(
                f"error routing to egg: {result} in session: {self.__session_id}"
            )

        return result

    def populate_processor(self, store: ErStore):
        populated_partitions = list()
        for p in store._partitions:
            server_node_id = p._processor._server_node_id
            rank_in_node = self.get_rank_in_node(p.id, p.processor._server_node_id)
            pp = ErPartition(
                id=p.id,
                store_locator=p.store_locator,
                processor=self.route_to_egg_by_rank(server_node_id, rank_in_node),
                rank_in_node=rank_in_node,
            )
            populated_partitions.append(pp)
        return ErStore(
            store_locator=store.store_locator,
            partitions=populated_partitions,
            options=store._options,
        )

    def get_or_create_store(self, store: ErStore):
        store = self._cluster_manager_client.get_or_create_store(store)
        return self.populate_processor(store)

    def stop(self):
        L.info(f"stopping session (gracefully): {self.__session_id}")
        L.debug(f"stopping session (gracefully), details: {self.__session_meta}")
        L.debug(f"stopping (gracefully) for {self.__session_id} from: {get_stack()}")
        self.run_exit_tasks()
        self.stopped = True
        return self._cluster_manager_client.stop_session(self.__session_meta)

    def kill(self):
        L.info(f"killing session (forcefully): {self.__session_id}")
        L.debug(f"killing session (forcefully), details: {self.__session_meta}")
        L.debug(f"killing (forcefully) for {self.__session_id} from: {get_stack()}")
        self.stopped = True

        future = self.executor.submit(self.stop)
        done = wait([future], timeout=1, return_when=FIRST_EXCEPTION).done
        if done:
            L.info(f"stopped successfully before kill session: {self.__session_id}")
        else:
            L.warning(f"stopped timeout before kill session: {self.__session_id}")

        return self._cluster_manager_client.kill_session(self.__session_meta)

    def get_session_id(self):
        return self.__session_id

    def get_session_meta(self):
        return self.__session_meta

    # todo:1: add_exit_task? not necessarily a cleanup semantic
    def add_exit_task(self, func):
        self.__exit_tasks.append(func)

    def run_exit_tasks(self):
        L.debug(f"running exit tasks: {self.__session_id}")
        for func in self.__exit_tasks:
            func()

    def get_option(self, key, default=None):
        return self.__options.get(key, default)

    def has_option(self, key):
        return self.__options.get(key) is not None

    def get_all_options(self):
        return self.__options.copy()

    def is_stopped(self):
        return self.stopped

    def get_eggs(self):
        return self._eggs.copy()

    def get_eggs_count(self):
        egg_count = 0
        for k, v in self._eggs.items():
            egg_count += len(v)
        return egg_count


def _calculate_rank_in_node(partition_id, cluster_node_count, processor_count_of_node):
    return (partition_id // cluster_node_count) % processor_count_of_node
