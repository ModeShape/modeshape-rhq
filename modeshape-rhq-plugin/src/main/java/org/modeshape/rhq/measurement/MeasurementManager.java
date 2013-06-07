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
package org.modeshape.rhq.measurement;

import java.util.Hashtable;
import javax.jcr.Repository;
import javax.naming.InitialContext;
import org.modeshape.jcr.api.Session;
import org.modeshape.jcr.api.Workspace;
import org.modeshape.jcr.api.monitor.DurationMetric;
import org.modeshape.jcr.api.monitor.History;
import org.modeshape.jcr.api.monitor.RepositoryMonitor;
import org.modeshape.jcr.api.monitor.Statistics;
import org.modeshape.jcr.api.monitor.ValueMetric;
import org.modeshape.jcr.api.monitor.Window;
import org.modeshape.rhq.PluginI18n;
import org.modeshape.rhq.RepositoryComponent;
import org.modeshape.rhq.util.I18n;
import org.modeshape.rhq.util.ToolBox;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;

/**
 * Responsible for obtaining metrics from the ModeShape server.
 */
public final class MeasurementManager {

    private RepositoryMonitor monitor;
    private final String name;
    private Repository repository;
    private final Window window;

    /**
     * Constructs a measurement manager with a window equal to the previous 60 seconds.
     * 
     * @param repositoryName the repository name whose metrics are needed (cannot be <code>null</code> or empty)
     * @throws Exception if there is a problem constructing the measurement connection with the ModeShape server
     */
    public MeasurementManager( final String repositoryName ) throws Exception {
        this(repositoryName, Window.PREVIOUS_60_SECONDS);
    }

    /**
     * @param repositoryName the repository name whose metrics are needed (cannot be <code>null</code> or empty)
     * @param measurementWindow the measurement window the metrics should pertain to (can be <code>null</code>)
     * @throws Exception if there is a problem constructing the measurement connection with the ModeShape server
     */
    public MeasurementManager( final String repositoryName,
                               final Window measurementWindow ) throws Exception {
        ToolBox.verifyNotEmpty(repositoryName, "repositoryName");
        this.name = repositoryName;
        this.window = ((measurementWindow == null) ? Window.PREVIOUS_60_SECONDS : measurementWindow);
    }

    /**
     * @param request the measurement request (cannot be <code>null</code>)
     * @return the measurement (can be <code>null</code>)
     * @throws Exception if a problem obtaining the measurement
     */
    public MeasurementData measurement( final MeasurementScheduleRequest request ) throws Exception {
        ToolBox.verifyNotNull(request, "request");

        final String metric = request.getName();
        History history = null;

        if (ValueMetric.fromLiteral(metric) != null) {
            history = monitor().getHistory(ValueMetric.fromLiteral(metric), this.window);
        } else if (DurationMetric.fromLiteral(metric) != null) {
            history = monitor().getHistory(DurationMetric.fromLiteral(metric), this.window);
        } else {
            throw new Exception(I18n.bind(PluginI18n.unknownMetric, request.getName(), this.name, RepositoryComponent.TYPE));
        }

        assert (history != null) : "measurement history is null";

        final Statistics[] stats = history.getStats();
        double value = -1;

        if ((stats.length != 0) && (stats[stats.length - 1] != null)) {
            value = stats[stats.length - 1].getMean();
        }

        final MeasurementData result = new MeasurementDataNumeric(history.getEndTime().getMilliseconds(), request, value);
        return result;
    }

    private RepositoryMonitor monitor() throws Exception {
        if (this.monitor == null) {
            // map.put(InitialContext.INITIAL_CONTEXT_FACTORY, "org.jboss.as.naming.InitialContextFactory");
            // map.put(InitialContext.PROVIDER_URL, "localhost:1099");

            final Hashtable<String, String> map = new Hashtable<String, String>();
            map.put(InitialContext.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
            map.put(InitialContext.PROVIDER_URL, "remote://localhost:4447");
            final InitialContext context = new InitialContext(map);
            final String jndiName = "java:/jcr/" + this.name;
            this.repository = (Repository)context.lookup(jndiName);
            //
            // Repository testRepository = null;
            // final String key = "org.modeshape.jcr.URL";
            // final String value = "jndi:jcr/" + this.name;
            // final Map<String, String> parameters = Collections.singletonMap(key, value);
            //
            // for (final RepositoryFactory factory : ServiceLoader.load(RepositoryFactory.class)) {
            // testRepository = factory.getRepository(parameters);
            //
            // if (testRepository != null) {
            // this.repository = testRepository;
            // break;
            // }
            // }

            if (this.repository == null) {
                throw new Exception(I18n.bind(PluginI18n.repositoryNotFound, this.name, jndiName));
            }

            final Session session = (Session)this.repository.login();
            final Workspace workspace = session.getWorkspace();
            this.monitor = workspace.getRepositoryManager().getRepositoryMonitor();
        }

        return this.monitor;
    }

}
