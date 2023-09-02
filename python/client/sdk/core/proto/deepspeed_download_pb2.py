# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: deepspeed_download.proto
"""Generated protocol buffer code."""
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from google.protobuf import duration_pb2 as google_dot_protobuf_dot_duration__pb2
import meta_pb2 as meta__pb2
import containers_pb2 as containers__pb2


DESCRIPTOR = _descriptor.FileDescriptor(
  name='deepspeed_download.proto',
  package='com.webank.eggroll.core.meta',
  syntax='proto3',
  serialized_options=None,
  create_key=_descriptor._internal_create_key,
  serialized_pb=b'\n\x18\x64\x65\x65pspeed_download.proto\x12\x1c\x63om.webank.eggroll.core.meta\x1a\x1egoogle/protobuf/duration.proto\x1a\nmeta.proto\x1a\x10\x63ontainers.proto\"\xad\x01\n\x16PrepareDownloadRequest\x12\x12\n\nsession_id\x18\x01 \x01(\t\x12\r\n\x05ranks\x18\x02 \x03(\x05\x12\x17\n\x0f\x63ompress_method\x18\x03 \x01(\t\x12\x16\n\x0e\x63ompress_level\x18\x04 \x01(\x05\x12?\n\x0c\x63ontent_type\x18\x05 \x01(\x0e\x32).com.webank.eggroll.core.meta.ContentType\">\n\x17PrepareDownloadResponse\x12\x12\n\nsession_id\x18\x01 \x01(\t\x12\x0f\n\x07\x63ontent\x18\x02 \x01(\t\"\xa8\x01\n\x11\x44sDownloadRequest\x12\x12\n\nsession_id\x18\x01 \x01(\t\x12\r\n\x05ranks\x18\x02 \x03(\x05\x12\x17\n\x0f\x63ompress_method\x18\x03 \x01(\t\x12\x16\n\x0e\x63ompress_level\x18\x04 \x01(\x05\x12?\n\x0c\x63ontent_type\x18\x05 \x01(\x0e\x32).com.webank.eggroll.core.meta.ContentType\"s\n\x12\x44sDownloadResponse\x12\x12\n\nsession_id\x18\x01 \x01(\t\x12I\n\x11\x63ontainer_content\x18\x02 \x03(\x0b\x32..com.webank.eggroll.core.meta.ContainerContent2\x84\x01\n\x11\x44sDownloadService\x12o\n\x08\x64ownload\x12/.com.webank.eggroll.core.meta.DsDownloadRequest\x1a\x30.com.webank.eggroll.core.meta.DsDownloadResponse\"\x00\x62\x06proto3'
  ,
  dependencies=[google_dot_protobuf_dot_duration__pb2.DESCRIPTOR, meta__pb2.DESCRIPTOR, containers__pb2.DESCRIPTOR,])




_PREPAREDOWNLOADREQUEST = _descriptor.Descriptor(
  name='PrepareDownloadRequest',
  full_name='com.webank.eggroll.core.meta.PrepareDownloadRequest',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='session_id', full_name='com.webank.eggroll.core.meta.PrepareDownloadRequest.session_id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='ranks', full_name='com.webank.eggroll.core.meta.PrepareDownloadRequest.ranks', index=1,
      number=2, type=5, cpp_type=1, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='compress_method', full_name='com.webank.eggroll.core.meta.PrepareDownloadRequest.compress_method', index=2,
      number=3, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='compress_level', full_name='com.webank.eggroll.core.meta.PrepareDownloadRequest.compress_level', index=3,
      number=4, type=5, cpp_type=1, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='content_type', full_name='com.webank.eggroll.core.meta.PrepareDownloadRequest.content_type', index=4,
      number=5, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=121,
  serialized_end=294,
)


_PREPAREDOWNLOADRESPONSE = _descriptor.Descriptor(
  name='PrepareDownloadResponse',
  full_name='com.webank.eggroll.core.meta.PrepareDownloadResponse',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='session_id', full_name='com.webank.eggroll.core.meta.PrepareDownloadResponse.session_id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='content', full_name='com.webank.eggroll.core.meta.PrepareDownloadResponse.content', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=296,
  serialized_end=358,
)


_DSDOWNLOADREQUEST = _descriptor.Descriptor(
  name='DsDownloadRequest',
  full_name='com.webank.eggroll.core.meta.DsDownloadRequest',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='session_id', full_name='com.webank.eggroll.core.meta.DsDownloadRequest.session_id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='ranks', full_name='com.webank.eggroll.core.meta.DsDownloadRequest.ranks', index=1,
      number=2, type=5, cpp_type=1, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='compress_method', full_name='com.webank.eggroll.core.meta.DsDownloadRequest.compress_method', index=2,
      number=3, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='compress_level', full_name='com.webank.eggroll.core.meta.DsDownloadRequest.compress_level', index=3,
      number=4, type=5, cpp_type=1, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='content_type', full_name='com.webank.eggroll.core.meta.DsDownloadRequest.content_type', index=4,
      number=5, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=361,
  serialized_end=529,
)


_DSDOWNLOADRESPONSE = _descriptor.Descriptor(
  name='DsDownloadResponse',
  full_name='com.webank.eggroll.core.meta.DsDownloadResponse',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='session_id', full_name='com.webank.eggroll.core.meta.DsDownloadResponse.session_id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='container_content', full_name='com.webank.eggroll.core.meta.DsDownloadResponse.container_content', index=1,
      number=2, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=531,
  serialized_end=646,
)

_PREPAREDOWNLOADREQUEST.fields_by_name['content_type'].enum_type = containers__pb2._CONTENTTYPE
_DSDOWNLOADREQUEST.fields_by_name['content_type'].enum_type = containers__pb2._CONTENTTYPE
_DSDOWNLOADRESPONSE.fields_by_name['container_content'].message_type = containers__pb2._CONTAINERCONTENT
DESCRIPTOR.message_types_by_name['PrepareDownloadRequest'] = _PREPAREDOWNLOADREQUEST
DESCRIPTOR.message_types_by_name['PrepareDownloadResponse'] = _PREPAREDOWNLOADRESPONSE
DESCRIPTOR.message_types_by_name['DsDownloadRequest'] = _DSDOWNLOADREQUEST
DESCRIPTOR.message_types_by_name['DsDownloadResponse'] = _DSDOWNLOADRESPONSE
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

PrepareDownloadRequest = _reflection.GeneratedProtocolMessageType('PrepareDownloadRequest', (_message.Message,), {
  'DESCRIPTOR' : _PREPAREDOWNLOADREQUEST,
  '__module__' : 'deepspeed_download_pb2'
  # @@protoc_insertion_point(class_scope:com.webank.eggroll.core.meta.PrepareDownloadRequest)
  })
_sym_db.RegisterMessage(PrepareDownloadRequest)

PrepareDownloadResponse = _reflection.GeneratedProtocolMessageType('PrepareDownloadResponse', (_message.Message,), {
  'DESCRIPTOR' : _PREPAREDOWNLOADRESPONSE,
  '__module__' : 'deepspeed_download_pb2'
  # @@protoc_insertion_point(class_scope:com.webank.eggroll.core.meta.PrepareDownloadResponse)
  })
_sym_db.RegisterMessage(PrepareDownloadResponse)

DsDownloadRequest = _reflection.GeneratedProtocolMessageType('DsDownloadRequest', (_message.Message,), {
  'DESCRIPTOR' : _DSDOWNLOADREQUEST,
  '__module__' : 'deepspeed_download_pb2'
  # @@protoc_insertion_point(class_scope:com.webank.eggroll.core.meta.DsDownloadRequest)
  })
_sym_db.RegisterMessage(DsDownloadRequest)

DsDownloadResponse = _reflection.GeneratedProtocolMessageType('DsDownloadResponse', (_message.Message,), {
  'DESCRIPTOR' : _DSDOWNLOADRESPONSE,
  '__module__' : 'deepspeed_download_pb2'
  # @@protoc_insertion_point(class_scope:com.webank.eggroll.core.meta.DsDownloadResponse)
  })
_sym_db.RegisterMessage(DsDownloadResponse)



_DSDOWNLOADSERVICE = _descriptor.ServiceDescriptor(
  name='DsDownloadService',
  full_name='com.webank.eggroll.core.meta.DsDownloadService',
  file=DESCRIPTOR,
  index=0,
  serialized_options=None,
  create_key=_descriptor._internal_create_key,
  serialized_start=649,
  serialized_end=781,
  methods=[
  _descriptor.MethodDescriptor(
    name='download',
    full_name='com.webank.eggroll.core.meta.DsDownloadService.download',
    index=0,
    containing_service=None,
    input_type=_DSDOWNLOADREQUEST,
    output_type=_DSDOWNLOADRESPONSE,
    serialized_options=None,
    create_key=_descriptor._internal_create_key,
  ),
])
_sym_db.RegisterServiceDescriptor(_DSDOWNLOADSERVICE)

DESCRIPTOR.services_by_name['DsDownloadService'] = _DSDOWNLOADSERVICE

# @@protoc_insertion_point(module_scope)
