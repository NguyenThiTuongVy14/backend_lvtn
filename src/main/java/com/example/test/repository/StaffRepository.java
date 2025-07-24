package com.example.test.repository;

import com.example.test.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Integer> {

    // ==== AUTHENTICATION METHODS ====

    // Tìm Staff theo username (cho authentication)
    Staff findByUserName(String userName);

    // Tìm Staff theo username với Optional
    Optional<Staff> findOptionalByUserName(String userName);

    // Tìm Staff theo email (có thể dùng email để login)
    Optional<Staff> findByEmail(String email);


    // Kiểm tra username có tồn tại không
    boolean existsByUserName(String userName);

    // Kiểm tra email có tồn tại không
    boolean existsByEmail(String email);

    // Tìm Staff theo username và status (chỉ user active)
    @Query("SELECT s FROM Staff s WHERE s.userName = :userName AND s.status = :status")
    Optional<Staff> findByUserNameAndStatus(@Param("userName") String userName, @Param("status") String status);

    // ==== AUTHORITY RELATED METHODS ====

    // Lấy tên Authority của một Staff theo username (cho authentication)
    @Query("SELECT a.name FROM Staff s JOIN Authority a ON s.authorityId = a.id WHERE s.userName = :userName")
    Optional<String> findAuthorityNameByUserName(@Param("userName") String userName);

    @Query("SELECT a.name FROM Staff s JOIN Authority a ON s.authorityId = a.id WHERE s.userName = :userName")
    String findAuthorityName(@Param("userName") String userName);
    // Lấy tên Authority của một Staff theo ID
    @Query("SELECT a.name FROM Staff s JOIN Authority a ON s.authorityId = a.id WHERE s.id = :staffId")
    Optional<String> findAuthorityNameByStaffId(@Param("staffId") Integer staffId);

    // Lấy Staff cùng với tên Authority theo username
    @Query("SELECT s.id, s.staffCode, s.fullName, s.userName, s.email, s.status, a.name " +
            "FROM Staff s JOIN Authority a ON s.authorityId = a.id WHERE s.userName = :userName")
    Optional<Object[]> findStaffWithAuthorityByUserName(@Param("userName") String userName);

    // Lấy Staff cùng với tên Authority theo ID
    @Query("SELECT s.id, s.staffCode, s.fullName, s.userName, s.email, s.status, a.name " +
            "FROM Staff s JOIN Authority a ON s.authorityId = a.id WHERE s.id = :staffId")
    Optional<Object[]> findStaffWithAuthorityName(@Param("staffId") Integer staffId);

    // Lấy tất cả Staff cùng với tên Authority
    @Query("SELECT s.id, s.staffCode, s.fullName, s.userName, s.email, s.status, a.name " +
            "FROM Staff s JOIN Authority a ON s.authorityId = a.id")
    List<Object[]> findAllStaffWithAuthorityName();

    // ==== SEARCH AND FILTER METHODS ====

    // Lấy danh sách Staff theo tên Authority
    @Query("SELECT s FROM Staff s JOIN Authority a ON s.authorityId = a.id WHERE a.name = :authorityName")
    List<Staff> findStaffByAuthorityName(@Param("authorityName") String authorityName);

    // Lấy Staff có Authority cụ thể và status active
    @Query("SELECT s FROM Staff s JOIN Authority a ON s.authorityId = a.id " +
            "WHERE a.name = :authorityName AND s.status = :status")
    List<Staff> findActiveStaffByAuthority(@Param("authorityName") String authorityName, @Param("status") String status);

    // Tìm kiếm Staff theo tên và Authority
    @Query("SELECT s FROM Staff s JOIN Authority a ON s.authorityId = a.id " +
            "WHERE s.fullName LIKE %:name% AND a.name = :authorityName")
    List<Staff> findStaffByNameAndAuthority(@Param("name") String name, @Param("authorityName") String authorityName);

    // Tìm kiếm Staff theo nhiều điều kiện
    @Query("SELECT s FROM Staff s WHERE " +
            "(:staffCode IS NULL OR s.staffCode LIKE %:staffCode%) AND " +
            "(:fullName IS NULL OR s.fullName LIKE %:fullName%) AND " +
            "(:status IS NULL OR s.status = :status)")
    List<Staff> findStaffByMultipleConditions(@Param("staffCode") String staffCode,
                                              @Param("fullName") String fullName,
                                              @Param("status") String status);

    // ==== STATISTICAL METHODS ====

    // Đếm số lượng Staff theo Authority
    @Query("SELECT a.name, COUNT(s) FROM Staff s JOIN Authority a ON s.authorityId = a.id GROUP BY a.name")
    List<Object[]> countStaffByAuthority();

    // Đếm số lượng Staff theo status
    @Query("SELECT s.status, COUNT(s) FROM Staff s GROUP BY s.status")
    List<Object[]> countStaffByStatus();

    // ==== NATIVE QUERY METHODS (nếu cần thiết) ====

    // Lấy tên Authority bằng Native Query
    @Query(value = "SELECT a.name FROM t_user s JOIN t_authority a ON s.authority_id = a.id WHERE s.user_name = :userName",
            nativeQuery = true)
    String findAuthorityNameByUserNameNative(@Param("userName") String userName);

    // Lấy Staff theo status
    List<Staff> findByStatus(String status);

    // Lấy Staff theo staffCode
    Optional<Staff> findByStaffCode(String staffCode);

    // Tìm Staff theo fullName chứa keyword
    List<Staff> findByFullNameContainingIgnoreCase(String keyword);

    // Lấy Staff có authorityId cụ thể
    List<Staff> findByAuthorityId(Integer authorityId);

    // Lấy Staff theo email và status
    Optional<Staff> findByEmailAndStatus(String email, String status);

    Staff findIdByUserName(String username);

    @Query("SELECT fullName FROM Staff where  id = :staffId")
    String findFullNameById(Integer staffId);

    @Modifying
    @Query("update Staff s set s.carryPoints = s.carryPoints + :delta where s.id = :id")
    void changeCarryPoints(@Param("id") Integer staffId, @Param("delta") int delta);

    @Modifying
    @Query("update Staff s set s.carryPoints = 0 where s.id = :id")
    void resetCarryPoints(@Param("id") Integer staffId);

}