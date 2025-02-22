/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.terracotta.test.util.TestBaseUtil;

import io.restassured.http.ContentType;
import com.tc.test.config.builder.ClusterManager;
import com.tc.test.config.builder.OffHeap;
import com.tc.test.config.builder.TcConfig;
import com.tc.test.config.builder.TcMirrorGroup;
import com.tc.test.config.builder.TcServer;
import com.tc.util.concurrent.ThreadUtil;

import static io.restassured.RestAssured.expect;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;

/**
 * CacheManagerLifecycleAgentResourceRESTTest
 * This test illustrates agent lifecycle compared to CacheManager one.
 */
public class CacheManagerLifecycleAgentResourceRESTTest {
    private static final String EXPECTED_RESOURCE_LOCATION = "/tc-management-api/agents";
    private static ClusterManager clusterManager;
    private CacheManager cacheManager;

    @BeforeClass
    public static void setUpClass() throws Exception {
        TcConfig tcConfig = new TcConfig()
                .mirrorGroup(
                        new TcMirrorGroup()
                                .server(
                                        new TcServer().managementPort(ResourceServiceImplITHelper.MANAGEMENT_PORT)
                                          .tsaGroupPort(ResourceServiceImplITHelper.TSA_GROUP_PORT)
                                          .offHeap(new OffHeap().enabled(true).maxDataSize("512m"))
                                )
                );

        TestBaseUtil.jarFor(ResourceServiceImplITHelper.class);
        tcConfig.fillUpConfig();

        clusterManager = new ClusterManager(CacheManagerLifecycleAgentResourceRESTTest.class, tcConfig);
        clusterManager.start();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        clusterManager.stop();
    }

    @Before
    public void createCacheManager() {
        cacheManager = CacheManager.newInstance(getConfiguration("cacheManagerOne"));
    }

    private Configuration getConfiguration(String name) {
        TerracottaClientConfiguration terracottaClientConfiguration = new TerracottaClientConfiguration();
        terracottaClientConfiguration.setUrl(ResourceServiceImplITHelper.CLUSTER_URL);
        return new Configuration().name(name).terracotta(terracottaClientConfiguration);
    }

    @After
    public void shutdownCacheManager() {
        cacheManager.shutdown();
    }

    @Test
    public void given_a_clustered_cache_manager_when_no_clustered_cache_then_L1_and_L2_agent_available() {
      expect().contentType(ContentType.JSON)
              .body("size()", is(2))
              .body("get(0).agencyOf", equalTo("TSA"))
              .body("get(1).agencyOf", equalTo("Ehcache"))
              .statusCode(200)
              .when().get(ResourceServiceImplITHelper.CLUSTERED_BASE_URL + EXPECTED_RESOURCE_LOCATION);
    }

    @Test
    public void given_two_clustered_cache_manager_when_shutting_one_down_then_L2_agent_and_L1_agent() {
        Configuration configuration = getConfiguration("cacheManagerTwo");
        configuration.addCache(getCacheConfiguration());
        cacheManager.addCache(new Cache(getCacheConfiguration()));
        CacheManager cacheManagerTwo = CacheManager.newInstance(configuration);
        try {
          ThreadUtil.reallySleep(1000);
          expect().contentType(ContentType.JSON)
                  .body("size()", is(3))
                  .statusCode(200)
                  .when().get(ResourceServiceImplITHelper.CLUSTERED_BASE_URL + EXPECTED_RESOURCE_LOCATION);
        } finally {
            cacheManagerTwo.shutdown();
        }
        expect().contentType(ContentType.JSON)
                .body("size()", is(2))
                .statusCode(200)
                .when().get(ResourceServiceImplITHelper.CLUSTERED_BASE_URL + EXPECTED_RESOURCE_LOCATION);
    }

    private CacheConfiguration getCacheConfiguration() {
        return new CacheConfiguration("test", 10).terracotta(new TerracottaConfiguration());
    }
}
