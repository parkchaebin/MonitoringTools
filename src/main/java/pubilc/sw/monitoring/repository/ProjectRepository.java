/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package pubilc.sw.monitoring.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pubilc.sw.monitoring.entity.ProjectEntity;

/**
 *
 * @author parkchaebin
 */
@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

    ProjectEntity findById(long pid);
    // 프로젝트에서 카테고리 가져오기
    @Query("SELECT p.category FROM ProjectEntity p WHERE p.id = :pid")
    Optional<String> findCategoryByProjectId(Long pid);
}
