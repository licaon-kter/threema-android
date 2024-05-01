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

package ch.threema.app.tasks

import ch.threema.app.managers.ServiceManager
import ch.threema.domain.models.MessageId
import ch.threema.domain.protocol.csp.messages.voip.VoipCallHangupMessage
import ch.threema.domain.protocol.csp.messages.voip.VoipCallHangupData
import ch.threema.domain.taskmanager.ActiveTaskCodec

class OutgoingVoipCallHangupMessageTask(
    private val voipCallHangupData: VoipCallHangupData,
    private val toIdentity: String,
    serviceManager: ServiceManager,
): OutgoingCspMessageTask(serviceManager) {
    override val type: String = "OutgoingVoipCallHangupMessageTask"

    override suspend fun invoke(handle: ActiveTaskCodec) {
        val message = VoipCallHangupMessage()
        message.setData(voipCallHangupData)
        message.toIdentity = toIdentity
        message.messageId = MessageId()

        sendContactMessage(message, null, handle)
    }

    // We do not need to persist this message
    override fun serialize(): SerializableTaskData? = null

}
