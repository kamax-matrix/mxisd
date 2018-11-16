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
    [ -n "$1" ] || exit 10
    [ "$1" = "$REQ_LOCALPART" ] || exit 20
    shift
fi

if [ -n "$WITH_DOMAIN" ]; then
    [ -n "$1" ] || exit 11
    [ "$1" = "$REQ_DOMAIN" ] || exit 21
    shift
fi

if [ -n "$WITH_MXID" ]; then
    [ -n "$1" ] || exit 12
    [ "$1" = "$REQ_MXID" ] || exit 22
    shift
fi

[ "$1" = "$REQ_PASS" ] || exit 1

exit 0
