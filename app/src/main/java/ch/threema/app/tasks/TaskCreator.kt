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
import ch.threema.domain.models.Contact
import ch.threema.domain.taskmanager.Task
import ch.threema.domain.taskmanager.TaskCodec
import ch.threema.protobuf.csp.e2e.fs.Terminate.Cause
import kotlinx.coroutines.Deferred

class TaskCreator(private val serviceManager: ServiceManager) {
    fun scheduleProfilePictureSendTaskAsync(toIdentity: String): Deferred<Unit> =
        scheduleTaskAsync {
            SendProfilePictureTask(toIdentity, serviceManager)
        }

    fun scheduleProfilePictureExecution(toIdentity: String): Deferred<Unit> = scheduleTaskAsync {
        ProfilePictureDistributionTask(toIdentity, serviceManager)
    }

    fun scheduleDeleteAndTerminateFSSessionsTaskAsync(
        contact: Contact,
        cause: Cause,
    ): Deferred<Unit> =
        scheduleTaskAsync {
            DeleteAndTerminateFSSessionsTask(
                serviceManager.forwardSecurityMessageProcessor,
                contact,
                cause
            )
        }

    fun scheduleSendPushTokenTask(token: String, type: Int): Deferred<Unit> =
        scheduleTaskAsync { SendPushTokenTask(token, type, serviceManager) }

    private fun <R> scheduleTaskAsync(createTask: () -> Task<R, TaskCodec>): Deferred<R> {
        return serviceManager.taskManager.schedule(createTask())
    }
}
