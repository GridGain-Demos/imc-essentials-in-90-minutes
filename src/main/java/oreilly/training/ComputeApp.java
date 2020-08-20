/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package oreilly.training;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import javax.cache.Cache;
import oreilly.training.model.TopCustomer;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

/**
 * The application uses Apache Ignite compute capabilities for a calculation of the top 5 paying customers. The compute
 * task executes on every cluster node, iterates through local records and responds to the application that merges partial
 * results.
 *
 * Finish the implementation of the compute task by completing the TODO task.
 */
public class ComputeApp {

    public static void main(String[] args) {
        Ignition.setClientMode(true);

        Ignite ignite = Ignition.start("ignite-config.xml");

        calculateTopPayingCustomers(ignite);

        ignite.close();
    }

    private static void calculateTopPayingCustomers(Ignite ignite) {
        ClusterGroup serversGroup = ignite.cluster().forServers();

        Collection<TreeSet<TopCustomer>> results = ignite.compute(
            serversGroup).broadcast(new TopPayingCustomersTask());

        printTop5PayingCustomers(results);
    }

    /**
     * Task that is executed on every cluster node and calculates top-5 local paying customers stored on a node.
     */
    private static class TopPayingCustomersTask implements IgniteCallable<TreeSet<TopCustomer>> {
        @IgniteInstanceResource
        private Ignite localNode;

        private HashMap<Integer, BigDecimal> customerPurchases = new HashMap<>();

        public TreeSet<TopCustomer> call() throws Exception {
            IgniteCache<BinaryObject, BinaryObject> invoiceLineCache = localNode.cache(
                "InvoiceLine").withKeepBinary();

            ScanQuery scanQuery = new ScanQuery();
            scanQuery.setLocal(true);

            QueryCursor<Cache.Entry<BinaryObject, BinaryObject>> cursor = invoiceLineCache.query(scanQuery);


            cursor.forEach(entry -> {
                BinaryObject val = entry.getValue();
                BinaryObject key = entry.getKey();

                /**
                 * TODO:
                 * initialize the <code>unitPrice</code> and <code>quantity</code>
                 * variables with the data from <code>val</code> variable.
                 *
                 * Rename this compute task so that the server nodes can load the logic without the cluster restart.
                 * That's just the specificity of this demo - the oreilly.training.ServerStartup added the class of this
                 * logic to its classpath upon the startup and, thus, won't load new versions of the class from the client
                 * apps like this one.
                 */
                BigDecimal unitPrice = new BigDecimal(0);
                int quantity = 0;

                processPurchase(key.field("customerId"), unitPrice.multiply(new BigDecimal(quantity)));
            });

            return calculateTopCustomers();
        }

        private void processPurchase(int itemId, BigDecimal price) {
            BigDecimal totalPrice = customerPurchases.get(itemId);

            if (totalPrice == null)
                customerPurchases.put(itemId, price);
            else
                customerPurchases.put(itemId, totalPrice.add(price));
        }

        private TreeSet<TopCustomer> calculateTopCustomers() {
            IgniteCache<Integer, BinaryObject> customersCache = localNode.cache("Customer").withKeepBinary();

            TreeSet<TopCustomer> sortedPurchases = new TreeSet<>();

            TreeSet<TopCustomer> top5 = new TreeSet<>();

            customerPurchases.entrySet().forEach(entry -> {
                TopCustomer topCustomer = new TopCustomer(entry.getKey(), entry.getValue());

                sortedPurchases.add(topCustomer);
            });

            Iterator<TopCustomer> iterator = sortedPurchases.descendingSet().iterator();

            int counter = 0;

            System.out.println(">>> Top 5 Customers: ");

            while (iterator.hasNext() && counter++ < 5) {
                TopCustomer customer = iterator.next();

                // It's safe to use localPeek because invoices are co-located with customer data.
                BinaryObject customerRecord = customersCache.localPeek(customer.getCustomerId(), CachePeekMode.PRIMARY);

                customer.setFullName(customerRecord.field("firstName") +
                    " " + customerRecord.field("lastName"));
                customer.setCity(customerRecord.field("city"));
                customer.setCountry(customerRecord.field("country"));

                top5.add(customer);

                System.out.println(customer);
            }

            return top5;
        }
    }

    private static void printTop5PayingCustomers(Collection<TreeSet<TopCustomer>> results) {
        System.out.println(">>> Top 5 Customers Across All Cluster Nodes");

        Iterator<TreeSet<TopCustomer>> iterator = results.iterator();

        TreeSet<TopCustomer> firstSet = iterator.next();

        while (iterator.hasNext())
            firstSet.addAll(iterator.next());

        Iterator<TopCustomer> customerIterator = firstSet.descendingSet().iterator();

        int counter = 0;

        while (customerIterator.hasNext() && counter++ < 5)
            System.out.println(customerIterator.next());
    }
}
