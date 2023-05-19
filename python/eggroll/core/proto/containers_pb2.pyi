"""
@generated by mypy-protobuf.  Do not edit manually!
isort:skip_file
"""
import builtins
import collections.abc
import google.protobuf.descriptor
import google.protobuf.internal.containers
import google.protobuf.message
import meta_pb2
import sys

if sys.version_info >= (3, 8):
    import typing as typing_extensions
else:
    import typing_extensions

DESCRIPTOR: google.protobuf.descriptor.FileDescriptor

@typing_extensions.final
class StartContainersRequest(google.protobuf.message.Message):
    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    @typing_extensions.final
    class EnvironmentVariablesEntry(google.protobuf.message.Message):
        DESCRIPTOR: google.protobuf.descriptor.Descriptor

        KEY_FIELD_NUMBER: builtins.int
        VALUE_FIELD_NUMBER: builtins.int
        key: builtins.str
        value: builtins.str
        def __init__(
            self,
            *,
            key: builtins.str = ...,
            value: builtins.str = ...,
        ) -> None: ...
        def ClearField(self, field_name: typing_extensions.Literal["key", b"key", "value", b"value"]) -> None: ...

    @typing_extensions.final
    class FilesEntry(google.protobuf.message.Message):
        DESCRIPTOR: google.protobuf.descriptor.Descriptor

        KEY_FIELD_NUMBER: builtins.int
        VALUE_FIELD_NUMBER: builtins.int
        key: builtins.str
        value: builtins.bytes
        def __init__(
            self,
            *,
            key: builtins.str = ...,
            value: builtins.bytes = ...,
        ) -> None: ...
        def ClearField(self, field_name: typing_extensions.Literal["key", b"key", "value", b"value"]) -> None: ...

    @typing_extensions.final
    class ZippedFilesEntry(google.protobuf.message.Message):
        DESCRIPTOR: google.protobuf.descriptor.Descriptor

        KEY_FIELD_NUMBER: builtins.int
        VALUE_FIELD_NUMBER: builtins.int
        key: builtins.str
        value: builtins.bytes
        def __init__(
            self,
            *,
            key: builtins.str = ...,
            value: builtins.bytes = ...,
        ) -> None: ...
        def ClearField(self, field_name: typing_extensions.Literal["key", b"key", "value", b"value"]) -> None: ...

    @typing_extensions.final
    class OptionsEntry(google.protobuf.message.Message):
        DESCRIPTOR: google.protobuf.descriptor.Descriptor

        KEY_FIELD_NUMBER: builtins.int
        VALUE_FIELD_NUMBER: builtins.int
        key: builtins.str
        value: builtins.str
        def __init__(
            self,
            *,
            key: builtins.str = ...,
            value: builtins.str = ...,
        ) -> None: ...
        def ClearField(self, field_name: typing_extensions.Literal["key", b"key", "value", b"value"]) -> None: ...

    SESSION_ID_FIELD_NUMBER: builtins.int
    NAME_FIELD_NUMBER: builtins.int
    JOB_TYPE_FIELD_NUMBER: builtins.int
    WORLD_SIZE_FIELD_NUMBER: builtins.int
    COMMAND_ARGUMENTS_FIELD_NUMBER: builtins.int
    ENVIRONMENT_VARIABLES_FIELD_NUMBER: builtins.int
    FILES_FIELD_NUMBER: builtins.int
    ZIPPED_FILES_FIELD_NUMBER: builtins.int
    OPTIONS_FIELD_NUMBER: builtins.int
    STATUS_FIELD_NUMBER: builtins.int
    PROCESSORS_FIELD_NUMBER: builtins.int
    session_id: builtins.str
    name: builtins.str
    job_type: builtins.str
    world_size: builtins.int
    @property
    def command_arguments(self) -> google.protobuf.internal.containers.RepeatedScalarFieldContainer[builtins.str]: ...
    @property
    def environment_variables(self) -> google.protobuf.internal.containers.ScalarMap[builtins.str, builtins.str]: ...
    @property
    def files(self) -> google.protobuf.internal.containers.ScalarMap[builtins.str, builtins.bytes]: ...
    @property
    def zipped_files(self) -> google.protobuf.internal.containers.ScalarMap[builtins.str, builtins.bytes]: ...
    @property
    def options(self) -> google.protobuf.internal.containers.ScalarMap[builtins.str, builtins.str]: ...
    status: builtins.str
    @property
    def processors(self) -> google.protobuf.internal.containers.RepeatedCompositeFieldContainer[meta_pb2.Processor]: ...
    def __init__(
        self,
        *,
        session_id: builtins.str = ...,
        name: builtins.str = ...,
        job_type: builtins.str = ...,
        world_size: builtins.int = ...,
        command_arguments: collections.abc.Iterable[builtins.str] | None = ...,
        environment_variables: collections.abc.Mapping[builtins.str, builtins.str] | None = ...,
        files: collections.abc.Mapping[builtins.str, builtins.bytes] | None = ...,
        zipped_files: collections.abc.Mapping[builtins.str, builtins.bytes] | None = ...,
        options: collections.abc.Mapping[builtins.str, builtins.str] | None = ...,
        status: builtins.str = ...,
        processors: collections.abc.Iterable[meta_pb2.Processor] | None = ...,
    ) -> None: ...
    def ClearField(self, field_name: typing_extensions.Literal["command_arguments", b"command_arguments", "environment_variables", b"environment_variables", "files", b"files", "job_type", b"job_type", "name", b"name", "options", b"options", "processors", b"processors", "session_id", b"session_id", "status", b"status", "world_size", b"world_size", "zipped_files", b"zipped_files"]) -> None: ...

global___StartContainersRequest = StartContainersRequest

@typing_extensions.final
class StartContainersResponse(google.protobuf.message.Message):
    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    SESSION_ID_FIELD_NUMBER: builtins.int
    session_id: builtins.str
    def __init__(
        self,
        *,
        session_id: builtins.str = ...,
    ) -> None: ...
    def ClearField(self, field_name: typing_extensions.Literal["session_id", b"session_id"]) -> None: ...

global___StartContainersResponse = StartContainersResponse

@typing_extensions.final
class StopContainersRequest(google.protobuf.message.Message):
    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    SESSION_ID_FIELD_NUMBER: builtins.int
    session_id: builtins.str
    def __init__(
        self,
        *,
        session_id: builtins.str = ...,
    ) -> None: ...
    def ClearField(self, field_name: typing_extensions.Literal["session_id", b"session_id"]) -> None: ...

global___StopContainersRequest = StopContainersRequest

@typing_extensions.final
class StopContainersResponse(google.protobuf.message.Message):
    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    def __init__(
        self,
    ) -> None: ...

global___StopContainersResponse = StopContainersResponse

@typing_extensions.final
class KillContainersRequest(google.protobuf.message.Message):
    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    SESSION_ID_FIELD_NUMBER: builtins.int
    session_id: builtins.str
    def __init__(
        self,
        *,
        session_id: builtins.str = ...,
    ) -> None: ...
    def ClearField(self, field_name: typing_extensions.Literal["session_id", b"session_id"]) -> None: ...

global___KillContainersRequest = KillContainersRequest

@typing_extensions.final
class KillContainersResponse(google.protobuf.message.Message):
    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    def __init__(
        self,
    ) -> None: ...

global___KillContainersResponse = KillContainersResponse

@typing_extensions.final
class DownloadContainersRequest(google.protobuf.message.Message):
    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    SESSION_ID_FIELD_NUMBER: builtins.int
    CONTAINER_IDS_FIELD_NUMBER: builtins.int
    COMPRESS_METHOD_FIELD_NUMBER: builtins.int
    session_id: builtins.str
    @property
    def container_ids(self) -> google.protobuf.internal.containers.RepeatedScalarFieldContainer[builtins.int]: ...
    compress_method: builtins.str
    def __init__(
        self,
        *,
        session_id: builtins.str = ...,
        container_ids: collections.abc.Iterable[builtins.int] | None = ...,
        compress_method: builtins.str = ...,
    ) -> None: ...
    def ClearField(self, field_name: typing_extensions.Literal["compress_method", b"compress_method", "container_ids", b"container_ids", "session_id", b"session_id"]) -> None: ...

global___DownloadContainersRequest = DownloadContainersRequest

@typing_extensions.final
class DownloadContainersResponse(google.protobuf.message.Message):
    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    SESSION_ID_FIELD_NUMBER: builtins.int
    CONTAINER_CONTENT_FIELD_NUMBER: builtins.int
    session_id: builtins.str
    @property
    def container_content(self) -> google.protobuf.internal.containers.RepeatedCompositeFieldContainer[global___ContainerContent]: ...
    def __init__(
        self,
        *,
        session_id: builtins.str = ...,
        container_content: collections.abc.Iterable[global___ContainerContent] | None = ...,
    ) -> None: ...
    def ClearField(self, field_name: typing_extensions.Literal["container_content", b"container_content", "session_id", b"session_id"]) -> None: ...

global___DownloadContainersResponse = DownloadContainersResponse

@typing_extensions.final
class ContainerContent(google.protobuf.message.Message):
    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    CONTAINER_ID_FIELD_NUMBER: builtins.int
    CONTENT_FIELD_NUMBER: builtins.int
    COMPRESS_METHOD_FIELD_NUMBER: builtins.int
    container_id: builtins.int
    content: builtins.bytes
    compress_method: builtins.str
    def __init__(
        self,
        *,
        container_id: builtins.int = ...,
        content: builtins.bytes = ...,
        compress_method: builtins.str = ...,
    ) -> None: ...
    def ClearField(self, field_name: typing_extensions.Literal["compress_method", b"compress_method", "container_id", b"container_id", "content", b"content"]) -> None: ...

global___ContainerContent = ContainerContent
