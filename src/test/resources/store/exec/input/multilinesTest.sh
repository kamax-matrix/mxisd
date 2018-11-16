#!/usr/bin/env bash
#
# mxisd - Matrix Identity Server Daemon
# Copyright (C) 2018 Kamax Sarl
#
# https://www.kamax.io/
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

if [ -n "$WITH_LOCALPART" ]; then
    read LOCALPART
    [ -n "$LOCALPART" ] || exit 10
    [ "$LOCALPART" = "$REQ_LOCALPART" ] || exit 20
fi

if [ -n "$WITH_DOMAIN" ]; then
    read DOMAIN
    [ -n "$DOMAIN" ] || exit 11
    [ "$DOMAIN" = "$REQ_DOMAIN" ] || exit 21
fi

if [ -n "$WITH_MXID" ]; then
    read MXID
    [ -n "$MXID" ] || exit 12
    [ "$MXID" = "$REQ_MXID" ] || exit 22
fi

read PASS
[ "$PASS" = "$REQ_PASS" ] || exit 1

exit 0
