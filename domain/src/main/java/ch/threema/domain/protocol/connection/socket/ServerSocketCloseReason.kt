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

package ch.threema.domain.protocol.connection.socket

/**
 * The reason for closing the socket.
 * It is also possible to indicate whether it is allowed to reconnect after the connection has been
 * closed. If this flag is set to `false` no reconnects will be attempted. If set to `true` reconnects
 * _might_ be attempted if it has not been prohibited by other mechanisms (e.g. the connection monitoring).
 *
 * @param msg A message explaining why the socket was closed. Only used for logging
 * @param reconnectAllowed Indicator whether it is allowed to attempt a reconnect
 */
open class ServerSocketCloseReason(val msg: String, val reconnectAllowed: Boolean? = null) {
    override fun toString(): String {
        return "ServerSocketCloseReason(msg='$msg', reconnectAllowed=$reconnectAllowed)"
    }
}
