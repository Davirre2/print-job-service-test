package com.adobe.printservice.repository;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, String> {

    @Query(value = "SELECT * FROM job WHERE status = :#{#status.name()} ORDER BY created_at ASC LIMIT 1 FOR UPDATE", nativeQuery = true)
    Optional<Job> findNextJobForUpdate(@Param("status") JobStatus status);

    boolean existsByStatus(JobStatus status);

    List<Job> findAllByOrderByCreatedAtAsc();

    List<Job> findAllByStatusOrderByCreatedAtAsc(JobStatus status);
}
