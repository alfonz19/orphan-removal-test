package alfonz19.orphanRemovalTest.jpa;

import javax.persistence.EntityManager;
import java.io.Serializable;

import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;



@SuppressWarnings("squid:S00119")
public class ExtendedJpaRepositoryImpl<T, ID extends Serializable>
        extends SimpleJpaRepository<T, ID>
        implements ExtendedJpaRepository<T, ID> {

    private final EntityManager em;

    public ExtendedJpaRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager em) {
        super(entityInformation, em);
        this.em = em;
    }

    @Transactional
    @Override
    public  <S extends T> S persist(S entity) {
        em.persist(entity);
        return entity;
    }

    @Transactional
    @Override
    public  <S extends T> S merge(S entity) {
        return em.merge(entity);
    }

    @Override
    public boolean isManaged(T entity) {
        return this.em.contains(entity);
    }
}