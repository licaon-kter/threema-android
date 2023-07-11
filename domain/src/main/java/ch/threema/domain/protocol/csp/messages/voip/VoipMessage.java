/*  _____ _
 * |_   _| |_  _ _ ___ ___ _ __  __ _
 *   | | | ' \| '_/ -_) -_) '  \/ _` |_
 *   |_| |_||_|_| \___\___|_|_|_\__,_(_)
 *
 * Threema for Android
 * Copyright (c) 2017-2023 Threema GmbH
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

package ch.threema.domain.protocol.csp.messages.voip;

import androidx.annotation.Nullable;
import ch.threema.domain.protocol.csp.messages.AbstractMessage;
import ch.threema.protobuf.csp.e2e.fs.Version;

public abstract class VoipMessage extends AbstractMessage {

	public VoipMessage() {
		super();
	}

	@Override
	public boolean flagSendPush() {
		return true;
	}

	@Override
	public boolean flagShortLivedServerQueuing() {
		// Should be set for all VoIP messages except for the hangup message
		return true;
	}

	@Nullable
	@Override
	public Version getMinimumRequiredForwardSecurityVersion() {
		return Version.V1_1;
	}
}
