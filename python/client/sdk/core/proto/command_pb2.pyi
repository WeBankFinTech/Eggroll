"""
@generated by mypy-protobuf.  Do not edit manually!
isort:skip_file

Copyright (c) 2019 - now, Eggroll Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""
import builtins
import collections.abc
import google.protobuf.descriptor
import google.protobuf.internal.containers
import google.protobuf.message
import sys

if sys.version_info >= (3, 8):
    import typing as typing_extensions
else:
    import typing_extensions

DESCRIPTOR: google.protobuf.descriptor.FileDescriptor

@typing_extensions.final
class CommandRequest(google.protobuf.message.Message):
    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    @typing_extensions.final
    class KwargsEntry(google.protobuf.message.Message):
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

    ID_FIELD_NUMBER: builtins.int
    URI_FIELD_NUMBER: builtins.int
    ARGS_FIELD_NUMBER: builtins.int
    KWARGS_FIELD_NUMBER: builtins.int
    id: builtins.str
    uri: builtins.str
    @property
    def args(self) -> google.protobuf.internal.containers.RepeatedScalarFieldContainer[builtins.bytes]: ...
    @property
    def kwargs(self) -> google.protobuf.internal.containers.ScalarMap[builtins.str, builtins.bytes]:
        """reserved for scala / python etc."""
    def __init__(
        self,
        *,
        id: builtins.str = ...,
        uri: builtins.str = ...,
        args: collections.abc.Iterable[builtins.bytes] | None = ...,
        kwargs: collections.abc.Mapping[builtins.str, builtins.bytes] | None = ...,
    ) -> None: ...
    def ClearField(self, field_name: typing_extensions.Literal["args", b"args", "id", b"id", "kwargs", b"kwargs", "uri", b"uri"]) -> None: ...

global___CommandRequest = CommandRequest

@typing_extensions.final
class CommandResponse(google.protobuf.message.Message):
    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    ID_FIELD_NUMBER: builtins.int
    REQUEST_FIELD_NUMBER: builtins.int
    RESULTS_FIELD_NUMBER: builtins.int
    id: builtins.str
    @property
    def request(self) -> global___CommandRequest: ...
    @property
    def results(self) -> google.protobuf.internal.containers.RepeatedScalarFieldContainer[builtins.bytes]: ...
    def __init__(
        self,
        *,
        id: builtins.str = ...,
        request: global___CommandRequest | None = ...,
        results: collections.abc.Iterable[builtins.bytes] | None = ...,
    ) -> None: ...
    def HasField(self, field_name: typing_extensions.Literal["request", b"request"]) -> builtins.bool: ...
    def ClearField(self, field_name: typing_extensions.Literal["id", b"id", "request", b"request", "results", b"results"]) -> None: ...

global___CommandResponse = CommandResponse
