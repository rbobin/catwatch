package org.zalando.catwatch.backend.repo;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.zalando.catwatch.backend.model.Contributor;
import org.zalando.catwatch.backend.model.ContributorKey;

public class ContributorRepositoryImpl implements ContributorRepositoryCustom {

	@PersistenceContext
	private EntityManager em;

	@Override
	public List<Contributor> findAllTimeTopContributors(Long organizationId, Date snapshotDate, Integer offset,
			Integer limit) {

		checkNotNull(snapshotDate, "snapshot date must not be null but was");

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Contributor> cq = cb.createQuery(Contributor.class);

		// define "from" and joins
		Root<Contributor> contributor = cq.from(Contributor.class);
		Path<ContributorKey> key = contributor.get("key");

		// define constraints
		List<Predicate> andPredicates = new ArrayList<Predicate>();
		if (organizationId != null) {
			andPredicates.add(cb.equal(key.get("organizationId"), organizationId));
		}
		 andPredicates.add(cb.equal(key.<Date>get("snapshotDate"),
		 snapshotDate));

		return em
				.createQuery(cq //
						.select(contributor) //
						.where(andPredicates.toArray(new Predicate[andPredicates.size()])) //
						.orderBy(cb.desc(contributor.get("organizationalCommitsCount"))))
				.setFirstResult(offset == null ? 0: offset) //
				.setMaxResults(limit == null ? 10000000 : limit) //
				.getResultList();
	}

	@Override
	public List<Contributor> findContributorsTimeSeries(Long organizationId, Date startDate, Date endDate,
			String namePrefix) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Contributor> cq = cb.createQuery(Contributor.class);

		// define "from" and joins
		Root<Contributor> contributor = cq.from(Contributor.class);
		Path<ContributorKey> key = contributor.get("key");

		// define constraints
		List<Predicate> andPredicates = new ArrayList<Predicate>();
		{
			if (organizationId != null) {
				andPredicates.add(cb.equal(key.get("organizationId"), organizationId));
			}
			if (startDate != null && endDate != null) {
				andPredicates.add(cb.between(key.<Date> get("snapshotDate"), startDate, endDate));
			} else if (startDate != null) {
				andPredicates.add(cb.<Date> greaterThanOrEqualTo(key.<Date> get("snapshotDate"), startDate));
			} else if (endDate != null) {
				andPredicates.add(cb.<Date> lessThanOrEqualTo(key.<Date> get("snapshotDate"), endDate));
			}
			if (namePrefix != null) {
				andPredicates.add(cb.like(contributor.get("name"), namePrefix + "%"));
			}
		}

		return em.createQuery(cq //
				.select(contributor) //
				.where(andPredicates.toArray(new Predicate[andPredicates.size()])) //
				.orderBy(cb.desc(key.get("snapshotDate")), cb.desc(key.get("id")))).getResultList();
	}

}
