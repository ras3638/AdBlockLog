/*
 * Copyright (C) 2010 Felix Bechstein
 * 
 * This file is part of AdBlock.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */

package de.ub0r.android.adBlock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * ProxyStarter listens to any Broadcast. It'l start the Proxy Service on
 * receive.
 * 
 * @author Felix Bechstein
 */
public class ProxyStarter extends BroadcastReceiver {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onReceive(final Context context, final Intent intent) {
		context.startService(new Intent(context, Proxy.class));
	}
}
