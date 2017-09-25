/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Maxime Dor
 *
 * https://max.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.mxisd.spring;

import io.kamax.mxisd.exception.ConfigurationException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class ConfigurationFailureAnalyzer extends AbstractFailureAnalyzer<ConfigurationException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, ConfigurationException cause) {
        String message = cause.getMessage();
        if (cause.getDetailedMessage().isPresent()) {
            message += " - " + cause.getDetailedMessage().get();
        }
        return new FailureAnalysis(message, "Double check the key value", cause);
    }

}
