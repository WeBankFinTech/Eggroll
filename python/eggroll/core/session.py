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
import os

from eggroll.core.client import ClusterManagerClient
from eggroll.core.conf_keys import SessionConfKeys
from eggroll.core.constants import SessionStatus, ProcessorTypes
from eggroll.core.meta_model import ErSessionMeta, \
    ErPartition
from eggroll.core.utils import get_self_ip, time_now

# TODO:1: support windows
# TODO:0: remove
if "EGGROLL_STANDALONE_DEBUG" not in os.environ:
    os.environ['EGGROLL_STANDALONE_DEBUG'] = "1"

class ErDeploy:
    pass

class ErSession(object):
    def __init__(self,
            session_id=f'er_session_py_{time_now()}_{get_self_ip()}',
            name='',
            tag='',
            processors=list(),
            options={}):
        self.__session_id = session_id
        self.__options = options.copy()
        self.__options[SessionConfKeys.CONFKEY_SESSION_ID] = self.__session_id
        self._cluster_manager_client = ClusterManagerClient(options=options)

        session_meta = ErSessionMeta(id=self.__session_id,
                                     name=name,
                                     status=SessionStatus.NEW,
                                     tag=tag,
                                     processors=processors,
                                     options=options)
        if not processors:
            self.__session_meta = self._cluster_manager_client.get_or_create_session(session_meta)
        else:
            self.__session_meta = self._cluster_manager_client.register_session(session_meta)

        self.__cleanup_tasks = list()
        self.__processors = self.__session_meta._processors

        print('session init finished')

        self._rolls = list()
        self._eggs = dict()

        for processor in self.__session_meta._processors:
            processor_type = processor._processor_type
            if processor_type == ProcessorTypes.EGG_PAIR:
                server_node_id = processor._server_node_id
                if server_node_id not in self._eggs:
                    self._eggs[server_node_id] = list()
                self._eggs[server_node_id].append(processor)
            elif processor_type == ProcessorTypes.ROLL_PAIR_MASTER:
                self._rolls.append(processor)
            else:
                raise ValueError(f"processor type {processor_type} not supported in roll pair")

    def route_to_egg(self, partition: ErPartition):
        target_server_node = partition._processor._server_node_id
        target_egg_processors = len(self._eggs[target_server_node])
        target_processor = (partition._id // target_egg_processors) % target_egg_processors

        return self._eggs[target_server_node][target_processor]

    def stop(self):
        return self._cluster_manager_client.stop_session(self.__session_meta)

    def get_session_id(self):
        return self.__session_id

    def get_session_meta(self):
        return self.__session_meta

    def add_cleanup_task(self, func):
        self.__cleanup_tasks.append(func)

    def run_cleanup_tasks(self):
        for func in self.__cleanup_tasks:
            func()

    def get_option(self, key, default=None):
        return self.__options.get(key, default)

    def has_option(self, key):
        return self.__options.get(key) is not None

    def get_all_options(self):
        return self.__options.copy()
