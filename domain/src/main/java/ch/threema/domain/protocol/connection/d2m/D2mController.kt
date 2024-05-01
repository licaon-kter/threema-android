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

package ch.threema.domain.protocol.connection.d2m

import ch.threema.domain.protocol.connection.SingleThreadedServerConnectionDispatcher
import ch.threema.domain.protocol.connection.csp.CspSession
import ch.threema.domain.protocol.connection.csp.CspSessionState
import ch.threema.domain.protocol.connection.util.MainConnectionController
import ch.threema.domain.protocol.connection.util.MdLayer3Controller
import ch.threema.domain.protocol.connection.util.MdLayer4Controller
import ch.threema.domain.protocol.connection.util.MdServerConnectionController
import kotlinx.coroutines.CompletableDeferred

internal class D2mController(configuration: D2mConnectionConfiguration) :
    MdServerConnectionController,
    MdLayer3Controller,
    MdLayer4Controller,
    MainConnectionController {

    override val dispatcher = SingleThreadedServerConnectionDispatcher(
        configuration.assertDispatcherContext
    )

    override val cspSession: CspSession = CspSession(configuration, dispatcher)
    override val d2mSession: D2mSession = D2mSession(configuration, dispatcher)

    override val cspSessionState: CspSessionState = cspSession

    override val connectionClosed = CompletableDeferred<Unit>()

    override val connected = CompletableDeferred<Unit>()

    override val cspAuthenticated = CompletableDeferred<Unit>()

    override val ioProcessingStoppedSignal = CompletableDeferred<Unit>()

    override val reflectionQueueDry  = CompletableDeferred<Unit>()
}
