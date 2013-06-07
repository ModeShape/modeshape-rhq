/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.rhq;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.modeshape.rhq.util.ToolBox;
import org.rhq.modules.plugins.jbossas7.json.Address;

/**
 * Represents an RHQ operation.
 */
class Operation {

    private final Map<String, String> args;
    private final String name;

    /**
     * @param operationName the operation name (cannot be <code>null</code> or empty)
     * @param operationArgs the operation arguments (can be <code>null</code>)
     */
    Operation( final String operationName,
               final String... operationArgs ) {
        ToolBox.verifyNotEmpty(operationName, "operationName");
        this.name = operationName;

        // add in arguments
        if ((operationArgs == null) || (operationArgs.length == 0)) {
            this.args = Collections.emptyMap();
        } else {
            this.args = new HashMap<String, String>((operationArgs.length / 2));

            for (int i = 0; i < operationArgs.length; ++i) {
                this.args.put(operationArgs[i++], operationArgs[i]);
            }
        }
    }

    /**
     * @return the arguments (never <code>null</code> but can be empty)
     */
    Map<String, String> args() {
        return this.args;
    }

    /**
     * @return the operation name (never <code>null</code> or empty)
     */
    String name() {
        return this.name;
    }

    /**
     * Common utilities associated with operations.
     */
    static class Util {

        /**
         * @param operation the operation being converted into an RHQ operation (cannot be <code>null</code>)
         * @return the RHQ operation (never <code>null</code>)
         */
        static org.rhq.modules.plugins.jbossas7.json.Operation createRhqOperation( final Operation operation ) {
            ToolBox.verifyNotNull(operation, "operation");

            final Address addr = ModeShapePlugin.createModeShapeAddress();
            final org.rhq.modules.plugins.jbossas7.json.Operation op = new org.rhq.modules.plugins.jbossas7.json.Operation(
                                                                                                                           operation.name(),
                                                                                                                           addr);

            if (operation.args() != null) {
                for (final Entry<String, String> prop : operation.args().entrySet()) {
                    op.addAdditionalProperty(prop.getKey(), prop.getValue());
                }
            }

            return op;
        }

    }

}
