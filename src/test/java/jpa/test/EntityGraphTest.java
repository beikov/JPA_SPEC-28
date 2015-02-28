package jpa.test;

import java.util.List;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Subgraph;
import javax.persistence.TypedQuery;
import jpa.test.entities.Address;
import jpa.test.entities.City;
import jpa.test.entities.User;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EntityGraphTest {
    
    private EntityManagerFactory emf;
    
    @Before
    public void setup() {
        emf = Persistence.createEntityManagerFactory("TestPU");
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        
        City c = new City("Vienna");
        Address a = new Address("Teststreet", c);
        User u = new User("Test", a);
        
        em.persist(c);
        em.persist(a);
        em.persist(u);
        
        tx.commit();
        em.close();
    }
    
    @After
    public void tearDown() {
        if (emf.isOpen()) {
            emf.close();
        }
    }
    
    @Test
    public void testGraph() {
        EntityManager em = emf.createEntityManager();
        EntityGraph<User> graph = em.createEntityGraph(User.class);
        graph.addAttributeNodes("name", "address");
        Subgraph<Address> addressGraph = graph.addSubgraph("address", Address.class);
        addressGraph.addAttributeNodes("street", "city");
        Subgraph<City> cityGraph = addressGraph.addSubgraph("city", City.class);
        cityGraph.addAttributeNodes("name");
        
        // Eclipselink issues 2 queries, fails to apply loadgraph
        // Hibernate issues 1 query, fails to apply loadgraph
        // Datanucleus issues 1 query, fails to apply loadgraph
        TypedQuery<User> query = em.createQuery("SELECT u FROM User u JOIN u.address a JOIN a.city c WHERE c.name LIKE 'Vi%'", User.class);
        query.setHint("javax.persistence.loadgraph", graph);
        List<User> users = query.getResultList();
        em.close();
        // Closing emf since eclipselink would do lazy loading even with closed entity manager!
        emf.close();
        
        Assert.assertEquals(1, users.size());
        // Test if everything was loaded as per JPA Spec 3.2.7
        Assert.assertEquals("Vienna", users.get(0).getAddress().getCity().getName());
    }
    
    @Test
    public void testJoinFetch() {
        EntityManager em = emf.createEntityManager();
        
        // Eclipselink issues 2 queries, all successful
        // Hibernate issues 1 query, all successful
        // Datanucleus issues 2 query, all successful
        TypedQuery<User> query = em.createQuery("SELECT u FROM User u JOIN FETCH u.address a JOIN FETCH a.city c WHERE c.name LIKE 'Vi%'", User.class);
        List<User> users = query.getResultList();
        em.close();
        // Closing emf since eclipselink would do lazy loading even with closed entity manager!
        emf.close();
        
        Assert.assertEquals(1, users.size());
        // Test if everything was loaded as per JPA Spec 3.2.7
        Assert.assertEquals("Vienna", users.get(0).getAddress().getCity().getName());
    }
}
