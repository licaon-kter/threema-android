/*  _____ _
 * |_   _| |_  _ _ ___ ___ _ __  __ _
 *   | | | ' \| '_/ -_) -_) '  \/ _` |_
 *   |_| |_||_|_| \___\___|_|_|_\__,_(_)
 *
 * Threema for Android
 * Copyright (c) 2023-2024 Threema GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.threema.domain.protocol.connection.data

import ch.threema.base.utils.toHexString
import ch.threema.domain.protocol.D2mPayloadType
import ch.threema.domain.protocol.connection.ServerConnectionException
import ch.threema.protobuf.d2m.MdD2M
import com.google.protobuf.ByteString
import java.nio.ByteBuffer
import java.nio.ByteOrder

class D2mContainer(val payloadType: UByte, val payload: ByteArray)
    : InboundL1Message,
    OutboundL2Message {
    override val type: String = "D2mContainer"

    val bytes: ByteArray
        get() {
            return byteArrayOf(payloadType.toByte()) + ByteArray(3) + payload
        }
}

class D2mProtocolException(msg: String) : ServerConnectionException(msg)

@JvmInline
value class DeviceId(val id: ULong)

fun DeviceId.leBytes(): ByteArray {
    return ByteBuffer.wrap(ByteArray(ULong.SIZE_BYTES))
        .order(ByteOrder.LITTLE_ENDIAN)
        .putLong(id.toLong())
        .array()
}

enum class DeviceSlotExpirationPolicy(val value: Int) {
    VOLATILE(0),
    PERSISTENT(1);

    companion object {
        fun fromProto(value: Int): DeviceSlotExpirationPolicy {
            return when (value) {
                0 -> VOLATILE
                1 -> PERSISTENT
                else -> throw D2mProtocolException("Unknown DeviceSlotExpirationPolicy `${value}`")
            }
        }
    }
}

enum class DeviceSlotState(val value: Int) {
    NEW(0),
    EXISTING(1);

    companion object {
        fun fromProto(value: Int): DeviceSlotState {
            return when (value) {
                0 -> NEW
                1 -> EXISTING
                else -> throw D2mProtocolException("Unknown DeviceSlotState `${value}`")
            }
        }
    }
}

sealed class OutboundD2mMessage(override val payloadType: UByte) :
    OutboundL3Message,
    OutboundL4Message,
    OutboundL5Message,
    OutboundMessage {
    abstract fun toContainer(): D2mContainer

    internal class ClientHello(
        private val version: UInt,
        private val response: ByteArray,
        private val deviceId: DeviceId,
        private val deviceSlotsExhaustedPolicy: DeviceSlotsExhaustedPolicy,
        private val deviceSlotExpirationPolicy: DeviceSlotExpirationPolicy,
        private val expectedDeviceSlotState: DeviceSlotState,
        private val encryptedDeviceInfo: ByteArray
    ) : OutboundD2mMessage(D2mPayloadType.CLIENT_HELLO) {
        enum class DeviceSlotsExhaustedPolicy(val value: Int) {
            REJECT(0),
            DROP_LEAST_RECENT(1);
        }

        override val type: String = "D2mClientHello"

        override fun toContainer(): D2mContainer {
            val hello = MdD2M.ClientHello.newBuilder()
                .setVersion(version.toInt())
                .setResponse(ByteString.copyFrom(response))
                .setDeviceId(deviceId.id.toLong())
                .setDeviceSlotsExhaustedPolicyValue(deviceSlotsExhaustedPolicy.value)
                .setDeviceSlotExpirationPolicyValue(deviceSlotExpirationPolicy.value)
                .setExpectedDeviceSlotStateValue(expectedDeviceSlotState.value)
                .setEncryptedDeviceInfo(ByteString.copyFrom(encryptedDeviceInfo))
                .build()
            return D2mContainer(
                payloadType,
                hello.toByteArray()
            )
        }

        override fun toString(): String {
            return "ClientHello(version=$version, response=${response.toHexString(4)}, deviceId=$deviceId, deviceSlotsExhaustedPolicy=$deviceSlotsExhaustedPolicy, deviceSlotExpirationPolicy=$deviceSlotExpirationPolicy, expectedDeviceSlotState=$expectedDeviceSlotState, encryptedDeviceInfo=${encryptedDeviceInfo.toHexString(4)}, type='$type')"
        }


    }

    class GetDevicesInfo : OutboundD2mMessage(D2mPayloadType.GET_DEVICES_INFO) {
        override val type: String = "D2mGetDevicesInfo"

        override fun toContainer(): D2mContainer {
            val getDevicesInfo = MdD2M.GetDevicesInfo.newBuilder().build()
            return D2mContainer(payloadType, getDevicesInfo.toByteArray())
        }
    }

    class DropDevice(val deviceId: DeviceId) : OutboundD2mMessage(D2mPayloadType.DROP_DEVICE), OutboundMessage {
        override val type: String = "D2mDropDevice"

        override fun toContainer(): D2mContainer {
            val dropDevice = MdD2M.DropDevice.newBuilder()
                .setDeviceId(deviceId.id.toLong())
                .build()
            return D2mContainer(payloadType, dropDevice.toByteArray())
        }
    }

    class BeginTransaction(
        private val encryptedScope: ByteArray,
        private val ttl: UInt
    ) : OutboundD2mMessage(D2mPayloadType.BEGIN_TRANSACTION) {
        override val type: String = "D2mBeginTransaction"

        override fun toContainer(): D2mContainer {
            val beginTransaction = MdD2M.BeginTransaction.newBuilder()
                .setEncryptedScope(ByteString.copyFrom(encryptedScope))
                .setTtl(ttl.toInt())
                .build()
            return D2mContainer(payloadType, beginTransaction.toByteArray())
        }
    }

    class SetSharedDeviceData(private val encryptedSharedDeviceData: ByteArray) : OutboundD2mMessage(D2mPayloadType.SET_SHARED_DEVICE_DATA) {
        override val type: String = "D2mSetSharedDeviceData"

        override fun toContainer(): D2mContainer {
            val data = MdD2M.SetSharedDeviceData.newBuilder()
                .setEncryptedSharedDeviceData(ByteString.copyFrom(encryptedSharedDeviceData))
                .build()
            return D2mContainer(payloadType, data.toByteArray())
        }
    }

    class CommitTransaction : OutboundD2mMessage(D2mPayloadType.COMMIT_TRANSACTION) {
        override val type: String = "D2mCommitTransaction"

        override fun toContainer(): D2mContainer {
            val commitTransaction = MdD2M.CommitTransaction.newBuilder()
                .build()
            return D2mContainer(payloadType, commitTransaction.toByteArray())
        }
    }
}

sealed class InboundD2mMessage(override val payloadType: UByte) : InboundL2Message, InboundL3Message, InboundL4Message, InboundMessage {
    companion object {
        fun decodeContainer(container: D2mContainer): InboundD2mMessage {
            return when (container.payloadType) {
                D2mPayloadType.SERVER_HELLO -> ServerHello.decodeContainer(container)
                D2mPayloadType.SERVER_INFO -> ServerInfo.decodeContainer(container)
                D2mPayloadType.REFLECTION_QUEUE_DRY -> ReflectionQueueDry.decodeContainer(container)
                D2mPayloadType.ROLE_PROMOTED_TO_LEADER -> RolePromotedToLeader.decodeContainer(container)
                D2mPayloadType.DEVICES_INFO -> DevicesInfo.decodeContainer(container)
                D2mPayloadType.DROP_DEVICE_ACK -> DropDeviceAck.decodeContainer(container)
                D2mPayloadType.BEGIN_TRANSACTION_ACK -> BeginTransactionAck.decodeContainer(container)
                D2mPayloadType.COMMIT_TRANSACTION_ACK -> CommitTransactionAck.decodeContainer(container)
                D2mPayloadType.TRANSACTION_REJECTED -> TransactionRejected.decodeContainer(container)
                D2mPayloadType.TRANSACTION_ENDED -> TransactionEnded.decodeContainer(container)
                else -> throw D2mProtocolException("Unsupported payload type `${container.payloadType.toHex()}`")
            }
        }
    }

    override fun toInboundMessage(): InboundMessage = this

    internal class ServerHello(
        val version: UInt,
        val esk: ByteArray,
        val challenge: ByteArray) : InboundD2mMessage(D2mPayloadType.SERVER_HELLO) {
        override val type: String = "D2mServerHello"

        companion object {
            fun decodeContainer(container: D2mContainer): ServerHello {
                if (container.payloadType != D2mPayloadType.SERVER_HELLO) {
                    throw D2mProtocolException("Invalid payload type `${container.payloadType}`")
                }
                val proto = MdD2M.ServerHello.parseFrom(container.payload)
                return ServerHello(
                    proto.version.toUInt(),
                    proto.esk.toByteArray(),
                    proto.challenge.toByteArray()
                )
            }
        }
    }

    class ServerInfo(
        val currentTime: ULong,
        val maxDeviceSlots: UInt,
        val deviceSlotState: DeviceSlotState,
        val encryptedSharedDeviceData: ByteArray,
        val reflectionQueueLength: UInt
    ) : InboundD2mMessage(D2mPayloadType.SERVER_INFO) {
        override val type: String = "D2mServerInfo"

        companion object {
            fun decodeContainer(container: D2mContainer): ServerInfo {
                if (container.payloadType != D2mPayloadType.SERVER_INFO) {
                    throw D2mProtocolException("Invalid payload type `${container.payloadType}`")
                }
                val proto = MdD2M.ServerInfo.parseFrom(container.payload)
                return ServerInfo(
                    proto.currentTime.toULong(),
                    proto.maxDeviceSlots.toUInt(),
                    DeviceSlotState.fromProto(proto.deviceSlotStateValue),
                    proto.encryptedSharedDeviceData.toByteArray(),
                    proto.reflectionQueueLength.toUInt()
                )
            }
        }
    }

    internal class ReflectionQueueDry : InboundD2mMessage(D2mPayloadType.REFLECTION_QUEUE_DRY) {
        override val type: String = "D2mReflectionQueueDry"

        companion object {
            fun decodeContainer(container: D2mContainer): ReflectionQueueDry {
                 if (container.payloadType != D2mPayloadType.REFLECTION_QUEUE_DRY) {
                     throw D2mProtocolException("Invalid payload type `${container.payloadType}`")
                 }
                return ReflectionQueueDry()
            }
        }
    }

    internal class RolePromotedToLeader : InboundD2mMessage(D2mPayloadType.ROLE_PROMOTED_TO_LEADER) {
        override val type: String = "D2mRolePromotedToLeader"

        companion object {
            fun decodeContainer(container: D2mContainer): RolePromotedToLeader {
                if (container.payloadType != D2mPayloadType.ROLE_PROMOTED_TO_LEADER) {
                    throw D2mProtocolException("Invalid payload type `${container.payloadType}`")
                }
                return RolePromotedToLeader()
            }
        }
    }

    class DevicesInfo(val augmentedDeviceInfo: Map<DeviceId, AugmentedDeviceInfo>) : InboundD2mMessage(D2mPayloadType.DEVICES_INFO) {
        override val type: String = "D2mDevicesInfo"

        data class AugmentedDeviceInfo (
            val encryptedDeviceInfo: ByteArray,
            val connectedSince: ULong,
            val lastDisconnectAt: ULong,
            val deviceSlotExpirationPolicy: DeviceSlotExpirationPolicy
        ) {
            companion object {
                fun fromProto(augmentedDeviceInfo: MdD2M.DevicesInfo.AugmentedDeviceInfo): AugmentedDeviceInfo {
                    return AugmentedDeviceInfo(
                        augmentedDeviceInfo.encryptedDeviceInfo.toByteArray(),
                        augmentedDeviceInfo.connectedSince.toULong(),
                        augmentedDeviceInfo.lastDisconnectAt.toULong(),
                        DeviceSlotExpirationPolicy.fromProto(augmentedDeviceInfo.deviceSlotExpirationPolicyValue)
                    )
                }
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is AugmentedDeviceInfo) return false

                if (!encryptedDeviceInfo.contentEquals(other.encryptedDeviceInfo)) return false
                if (connectedSince != other.connectedSince) return false
                if (lastDisconnectAt != other.lastDisconnectAt) return false
                if (deviceSlotExpirationPolicy != other.deviceSlotExpirationPolicy) return false

                return true
            }

            override fun hashCode(): Int {
                var result = encryptedDeviceInfo.contentHashCode()
                result = 31 * result + connectedSince.hashCode()
                result = 31 * result + lastDisconnectAt.hashCode()
                result = 31 * result + deviceSlotExpirationPolicy.hashCode()
                return result
            }
        }

        companion object {
            fun decodeContainer(container: D2mContainer): DevicesInfo {
                if (container.payloadType != D2mPayloadType.DEVICES_INFO) {
                    throw D2mProtocolException("Invalid payload type `${container.payloadType}`")
                }
                val proto = MdD2M.DevicesInfo.parseFrom(container.payload)
                val info = proto.augmentedDeviceInfoMap.entries.associate {
                    val deviceId = DeviceId(it.key.toULong())
                    val info = AugmentedDeviceInfo(
                        it.value.encryptedDeviceInfo.toByteArray(),
                        it.value.connectedSince.toULong(),
                        it.value.lastDisconnectAt.toULong(),
                        DeviceSlotExpirationPolicy.fromProto(it.value.deviceSlotExpirationPolicyValue)
                    )
                    deviceId to info
                }
                return DevicesInfo(info)
            }
        }
    }

    class DropDeviceAck(val deviceId: DeviceId) : InboundD2mMessage(D2mPayloadType.DROP_DEVICE_ACK) {
        override val type: String = "D2mDropDeviceAck"

        companion object {
            fun decodeContainer(container: D2mContainer): DropDeviceAck {
                if (container.payloadType != D2mPayloadType.DROP_DEVICE_ACK) {
                    throw D2mProtocolException("Invalid payload type `${container.payloadType}`")
                }
                val proto = MdD2M.DropDeviceAck.parseFrom(container.payload)
                return DropDeviceAck(DeviceId(proto.deviceId.toULong()))
            }
        }
    }

    class BeginTransactionAck : InboundD2mMessage(D2mPayloadType.BEGIN_TRANSACTION_ACK) {
        override val type: String = "D2mBeginTransactionAck"

        companion object {
            fun decodeContainer(container: D2mContainer): BeginTransactionAck {
                if (container.payloadType != D2mPayloadType.BEGIN_TRANSACTION_ACK) {
                    throw D2mProtocolException("Invalid payload type `${container.payloadType}`")
                }
                return BeginTransactionAck()
            }
        }
    }

    class CommitTransactionAck : InboundD2mMessage(D2mPayloadType.COMMIT_TRANSACTION_ACK) {
        override val type: String = "D2mCommitTransactionAck"

        companion object {
            fun decodeContainer(container: D2mContainer): CommitTransactionAck {
                if (container.payloadType != D2mPayloadType.COMMIT_TRANSACTION_ACK) {
                    throw D2mProtocolException("Invalid payload type `${container.payloadType}`")
                }
                return CommitTransactionAck()
            }
        }
    }

    class TransactionRejected(
        val deviceId: DeviceId,
        val encryptedScope: ByteArray
    ) : InboundD2mMessage(D2mPayloadType.TRANSACTION_REJECTED) {
        override val type: String = "D2mTransactionRejected"

        companion object {
            fun decodeContainer(container: D2mContainer): TransactionRejected {
                if (container.payloadType != D2mPayloadType.TRANSACTION_REJECTED) {
                    throw D2mProtocolException("Invalid payload type `${container.payloadType}`")
                }
                val proto = MdD2M.TransactionRejected.parseFrom(container.payload)
                return TransactionRejected(
                    DeviceId(proto.deviceId.toULong()),
                    proto.encryptedScope.toByteArray()
                )
            }
        }
    }

    class TransactionEnded(
        val deviceId: DeviceId,
        val encryptedScope: ByteArray
    ) : InboundD2mMessage(D2mPayloadType.TRANSACTION_ENDED) {
        override val type: String = "D2mTransactionEnded"

        companion object {
            fun decodeContainer(container: D2mContainer): TransactionEnded {
                if (container.payloadType != D2mPayloadType.TRANSACTION_ENDED) {
                    throw D2mProtocolException("Invalid payload type `${container.payloadType}`")
                }
                val proto = MdD2M.TransactionEnded.parseFrom(container.payload)
                return TransactionEnded(
                    DeviceId(proto.deviceId.toULong()),
                    proto.encryptedScope.toByteArray()
                )
            }
        }
    }

    // TODO(ANDR-2475): Reflected messages
    // class Reflect : OutboundD2mMessage
    // class ReflectAck : D2mMessage
    // class Reflected : D2mMessage
    // class ReflectedAck : OutboundD2mMessage
}
