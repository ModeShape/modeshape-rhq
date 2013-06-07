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

import java.util.Collection;
import java.util.Map;
import org.modeshape.jcr.api.monitor.Window;
import org.modeshape.rhq.measurement.MeasurementManager;
import org.modeshape.rhq.util.I18n;
import org.modeshape.rhq.util.ToolBox;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * The ModeShape repository resource component.
 */
public final class RepositoryComponent extends ModeShapeComponent {

    static final String DESCRIPTION = PluginI18n.repositoryDescription;
    static final String DISPLAY_NAME = PluginI18n.repositoryDisplayName;
    static final Operation GET_ACTIVE_SESSION_COUNT = new Operation("getActiveSessions");
    public static final String TYPE = "ModeShapeRepository";

    private boolean loaded = false;
    private MeasurementManager metricsMgr;

    /**
     * Constructs a repository component.
     */
    public RepositoryComponent() {
        super(TYPE, DISPLAY_NAME, DESCRIPTION);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.rhq.ModeShapeComponent#load()
     */
    @Override
    protected void load() throws Exception {
        final Result repoResult = getASConnection().execute(Operation.Util.createRhqOperation(EngineComponent.GET_REPOSITORIES));
        final Object tempResult = repoResult.getResult();

        if ((tempResult != null) && (tempResult instanceof Map<?, ?>)) {
            final Map<?, ?> repoMap = (Map<?, ?>)tempResult;
            final Object tempValues = repoMap.get(deploymentName());

            if ((tempValues != null) && (tempValues instanceof Map<?, ?>)) {
                loadProperties((Map<?, ?>)tempValues);
                this.loaded = true;
            }
        } else {
            ModeShapePlugin.LOG.error("Unable to obtain the ModeShape repositories");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.rhq.ModeShapeComponent#loadProperty(java.lang.String, java.util.Collection)
     */
    @Override
    protected void loadProperty( final String name,
                                 final Collection<?> valueList ) throws Exception {
        String mapName = null;
        String propName = null;

        if (RhqId.NODE_TYPES.equals(name)) {
            mapName = RhqId.CND_FILES;
            propName = RhqId.CND_URI;
        } else if (RhqId.PREDEFINED_WORKSPACE_NAMES.equals(name)) {
            mapName = RhqId.WORKSPACE_NAMES;
            propName = RhqId.WORKSPACE_NAME;
        } else if (RhqId.ANONYMOUS_ROLES.equals(name)) {
            mapName = RhqId.ROLES;
            propName = RhqId.ROLE;
        } else if (RhqId.WORKSPACES_INITIAL_CONTENT.equals(name)) {
            mapName = RhqId.CONTENT_MAPPINGS;
            propName = RhqId.INITIAL_CONTENT;
        }

        if (ToolBox.isEmpty(mapName)) {
            throw new Exception(I18n.bind(PluginI18n.unknownProperty, name, deploymentName(), type()));
        }

        final PropertyMap propMap = new PropertyMap(mapName);

        for (final Object obj : valueList) {
            // TODO this is overwriting existing so only one value is showing in list
            final Property cndProp = new PropertySimple(propName, obj.toString());
            propMap.put(cndProp);
        }

        final PropertyList values = new PropertyList(name);
        values.add(propMap);
        resourceConfiguration().put(values);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.rhq.ModeShapeComponent#loadProperty(java.lang.String, java.lang.String)
     */
    @Override
    protected void loadProperty( final String name,
                                 final String value ) throws Exception {
        // list properties without values must still be loaded as lists
        String mapName = null;
        String propName = null;

        if (RhqId.NODE_TYPES.equals(name)) {
            mapName = RhqId.CND_FILES;
            propName = RhqId.CND_URI;
        } else if (RhqId.PREDEFINED_WORKSPACE_NAMES.equals(name)) {
            mapName = RhqId.WORKSPACE_NAMES;
            propName = RhqId.WORKSPACE_NAME;
        } else if (RhqId.ANONYMOUS_ROLES.equals(name)) {
            mapName = RhqId.ROLES;
            propName = RhqId.ROLE;
        } else if (RhqId.WORKSPACES_INITIAL_CONTENT.equals(name)) {
            mapName = RhqId.CONTENT_MAPPINGS;
            propName = RhqId.INITIAL_CONTENT;
        } else {
            super.loadProperty(name, value);
        }

        // must be a list property getting set with a single or no value
        final PropertyMap propMap = new PropertyMap(mapName);
        final Property cndProp = new PropertySimple(propName, value);
        propMap.put(cndProp);

        final PropertyList values = new PropertyList(propName);
        values.add(propMap);
        resourceConfiguration().put(values);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.rhq.ModeShapeComponent#metric(org.rhq.core.domain.measurement.MeasurementScheduleRequest)
     */
    @Override
    protected MeasurementData metric( final MeasurementScheduleRequest request ) throws Exception {
        final MeasurementManager metricsMgr = metricsManager();
        return metricsMgr.measurement(request);
    }

    private MeasurementManager metricsManager() throws Exception {
        if (this.metricsMgr == null) {
            this.metricsMgr = new MeasurementManager(deploymentName(), Window.PREVIOUS_60_SECONDS);
        }

        return this.metricsMgr;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.rhq.ModeShapeComponent#shouldLoad()
     */
    @Override
    protected boolean shouldLoad() {
        return !this.loaded;
    }

    /**
     * Identifiers that match the <code>rhq-plugin.xml</code> identifiers.
     */
    interface RhqId {
        String ANONYMOUS_ROLES = "anonymous-roles";
        String CND_FILES = "cnd-files";
        String CND_URI = "cnd-uri";
        String CONTENT_MAPPINGS = "content-mappings";
        String INITIAL_CONTENT = "initial-content";
        String NODE_TYPES = "node-types";
        String PREDEFINED_WORKSPACE_NAMES = "predefined-workspace-names";
        String ROLE = "role";
        String ROLES = "roles";
        String WORKSPACE_NAME = "workspace-name";
        String WORKSPACE_NAMES = "workspace-names";
        String WORKSPACES_INITIAL_CONTENT = "workspaces-initial-content";
    }

}
