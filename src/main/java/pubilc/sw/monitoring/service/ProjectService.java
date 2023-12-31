/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package pubilc.sw.monitoring.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import pubilc.sw.monitoring.dto.ProjectDTO;
import pubilc.sw.monitoring.entity.ProjectEntity;
import pubilc.sw.monitoring.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import pubilc.sw.monitoring.dto.MemberDTO;
import pubilc.sw.monitoring.dto.UserDTO;
import pubilc.sw.monitoring.entity.MemberEntity;
import pubilc.sw.monitoring.repository.MemberRepository;
import pubilc.sw.monitoring.repository.UserRepository;
import pubilc.sw.monitoring.entity.UserEntity;

/**
 *
 * @author parkchaebin
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;

    /**
     * 프로젝트 추가 함수 (project 테이블 및 member 테이블에 정보 추가)
     *
     * @param projectDTO 프로젝트 정보를 담은 ProjectDTO 객체
     * @param uid 사용자 아이디
     * @return (0 : 날짜 비교 실패, 1 : 추가 성공, 2 : 추가 실패)
     */
    public boolean addProject(ProjectDTO projectDTO, String uid) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate startDate = LocalDate.parse(projectDTO.getStart(), formatter);
        LocalDate endDate = LocalDate.parse(projectDTO.getEnd(), formatter);

        ProjectEntity addEntity = ProjectEntity.builder()
                .name(projectDTO.getName())
                .content(projectDTO.getContent())
                .start(java.sql.Date.valueOf(startDate))
                .end(java.sql.Date.valueOf(endDate))
                .category("공지사항")
                .cycle(projectDTO.getCycle())
                .build();

        addEntity = projectRepository.save(addEntity);

        if (addEntity != null) {
            // member 테이블에 정보 추가
            MemberEntity memberEntity = MemberEntity.builder()
                    .uid(uid)
                    .pid(addEntity.getId()) // 새로 추가된 프로젝트 아이디
                    .right(1) // 권한 정보 
                    .state(0) // 프로젝트 생성자는 초대수락여부 0로 설정  
                    .build();

            memberRepository.save(memberEntity);
            return true; // 추가 성공
        } else {
            return false; // 추가 실패
        }
    }

    /**
     * 본인이 해당하는 프로젝트 얻기 위한 함수
     *
     * @param uid 본인이 해당하는 프로젝트를 얻기 위한 사용자 아이디
     * @return 해당 사용자의 프로젝트 정보를 담은 DTO 리스트
     */
    public Page<ProjectDTO> getProjectsByUserId(String uid, int nowPage, String name) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 사용자 아이디를 기반으로 프로젝트 멤버 조회 
        List<MemberEntity> memberEntities = memberRepository.findByUid(uid);
        List<ProjectDTO> projectDTOs = new ArrayList<>();  // 프로젝트 엔티티 리스트 
        List<Long> projectIdList = new ArrayList();
        for (MemberEntity memberEntity : memberEntities) {
            if (memberEntity.getState() == 0 || memberEntity.getState() == 2) {
                projectIdList.add(memberEntity.getPid());
            }
        }
        Page<ProjectEntity> projectEntityList;
        if (name.equals("") || name == null) {
            projectEntityList = projectRepository.findByIds(projectIdList, PageRequest.of(nowPage - 1, 5, Sort.by(Sort.Direction.DESC, "end")));
        } else {
            projectEntityList = projectRepository.findByIdsAndName(projectIdList, name, PageRequest.of(nowPage - 1, 5, Sort.by(Sort.Direction.DESC, "end")));
        }

        for (ProjectEntity projectEntity : projectEntityList.getContent()) {
            // 프로젝트의 시작 및 종료 기간을 Instant 객체로 변환 
            Instant startInstant = projectEntity.getStart().toInstant();
            Instant endInstant = projectEntity.getEnd().toInstant();
            // Instant 객체를 시스템 기본 시간대를 사용하여 LocalDate로 변환
            LocalDate startDate = startInstant.atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate endDate = endInstant.atZone(ZoneId.systemDefault()).toLocalDate();

            // 프로젝트 정보를 DTO로 변환하여 리스트에 추가
            ProjectDTO projectDTO = ProjectDTO.builder()
                    .pid(projectEntity.getId())
                    .name(projectEntity.getName())
                    .content(projectEntity.getContent())
                    .start(projectEntity.getStart().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)) // 프로젝트의 시작 및 종료 기간을 Instant 객체로 변환 후 Instant 객체를 시스템 기본 시간대를 사용하여 LocalDate로 변환
                    .end(projectEntity.getEnd().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(formatter))
                    .category(projectEntity.getCategory())
                    .cycle(projectEntity.getCycle())
                    .build();
            projectDTOs.add(projectDTO);
        }
//        for (MemberEntity memberEntity : memberEntities) {
//            if (memberEntity.getState() == 0 || memberEntity.getState() == 2) {  // 초대 상태가 0(생성자)이거나 2(초대 수락)인 프로젝트
//                // 본인이 해당하는 프로젝트 아이디 
//                long pid = memberEntity.getPid();
//                ProjectEntity projectEntity = projectRepository.findById(pid);
//
//                if (projectEntity != null) {
//                    // 프로젝트의 시작 및 종료 기간을 Instant 객체로 변환 
//                    Instant startInstant = projectEntity.getStart().toInstant();
//                    Instant endInstant = projectEntity.getEnd().toInstant();
//                    // Instant 객체를 시스템 기본 시간대를 사용하여 LocalDate로 변환
//                    LocalDate startDate = startInstant.atZone(ZoneId.systemDefault()).toLocalDate();
//                    LocalDate endDate = endInstant.atZone(ZoneId.systemDefault()).toLocalDate();
//
//                    // 프로젝트 정보를 DTO로 변환하여 리스트에 추가
//                    ProjectDTO projectDTO = ProjectDTO.builder()
//                            .pid(projectEntity.getId())
//                            .name(projectEntity.getName())
//                            .content(projectEntity.getContent())
//                            .start(projectEntity.getStart().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)) // 프로젝트의 시작 및 종료 기간을 Instant 객체로 변환 후 Instant 객체를 시스템 기본 시간대를 사용하여 LocalDate로 변환
//                            .end(projectEntity.getEnd().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(formatter))
//                            .category(projectEntity.getCategory())
//                            .cycle(projectEntity.getCycle())
//                            .build();
//                    projectDTOs.add(projectDTO);
//                }
//            }
//        }

        return new PageImpl<>(projectDTOs, projectEntityList.getPageable(), projectEntityList.getTotalElements());
    }

    /**
     * 프로젝트 상세 정보 얻기 위한 함수
     *
     * @param pid 상세 정보를 얻을 프로젝트 아이디
     * @return 프로젝트 상세 정보를 담은 ProjectDTO 객체
     */
    public ProjectDTO getProjectDetails(Long pid) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 프로젝트 아이디 기반으로 프로젝트 조회 
        Optional<ProjectEntity> projectEntityOptional = projectRepository.findById(pid);

        if (projectEntityOptional.isPresent()) {  // 조회한 엔티티 존재 여부 확인 
            ProjectEntity projectEntity = projectEntityOptional.get();

            // 프로젝트의 시작 및 종료 기를 Instant 객체로 변환
            Instant startInstant = projectEntity.getStart().toInstant();
            Instant endInstant = projectEntity.getEnd().toInstant();
            // Instant 객체를 시스템 기본 시간대를 사용하여 LocalDate로 변환
            LocalDate startDate = startInstant.atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate endDate = endInstant.atZone(ZoneId.systemDefault()).toLocalDate();

            // ProjectDTO 객체 생성
            ProjectDTO projectDTO = ProjectDTO.builder()
                    .pid(projectEntity.getId())
                    .name(projectEntity.getName())
                    .content(projectEntity.getContent())
                    .start(startDate.format(formatter))
                    .end(endDate.format(formatter))
                    .category(projectEntity.getCategory())
                    .cycle(projectEntity.getCycle())
                    .build();

            // 카테고리 콤마로 나눠서 카테고리 리스트에 저장
            String category = projectEntity.getCategory();
            if (category != null & !category.isEmpty()) {
                String[] categories = category.split(",");
                List<String> categoryList = new ArrayList<>();
                for (String cat : categories) {
                    categoryList.add(cat.trim());
                }
                projectDTO.setCategoryList(categoryList);
            }

            return projectDTO;
        } else {
            return null;  // 해당 프로젝트가 존재하지 않는 경우
        }
    }

    /**
     * 프로젝트에 대한 수정 및 삭제에 대한 권한 확인 (member_right=1인 경우만 수정 및 삭제 가능)
     *
     * @param uid 사용자 아이디
     * @param pid 프로젝트 아이디
     * @return 권한 (1:마스터, 2:게시물 작성 및 편집, 3:보기권한)
     */
    public int hasRight(String uid, Long pid) {
        MemberEntity memberEntity = memberRepository.findByUidAndPid(uid, pid);
        if(memberEntity == null){
            return -1;
        }
        return memberEntity.getRight();
    }
//    public boolean hasRight(String uid, Long pid) {
//        MemberEntity memberEntity = memberRepository.findByUidAndPid(uid, pid);
//        return memberEntity != null && memberEntity.getRight() == 1;
//    }

    /**
     * 프로젝트 정보 수정 함수
     *
     * @param projectDTO 수정할 프로젝트 정보를 담은 ProjectDTO 객체
     * @return 수정 성공 여부 (0 : 날짜 비교 실패, 1 : 수정 성공, 2 : 수정 실패 또는 수정할 프로젝트가 존재하지
     * 않음)
     */
    public boolean updateProject(ProjectDTO projectDTO) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 프로젝트 아이디를 기반으로 프로젝트 조회 
        Optional<ProjectEntity> projectEntityOptional = projectRepository.findById(projectDTO.getPid());

        if (projectEntityOptional.isPresent()) {  // 조회한 엔티티 존재 여부 확인 
            ProjectEntity projectEntity = projectEntityOptional.get();
            LocalDate startDate = LocalDate.parse(projectDTO.getStart(), formatter);
            LocalDate endDate = LocalDate.parse(projectDTO.getEnd(), formatter);

            // 엔티티 정보를 업데이트하여 새로운 엔티티 생성
            ProjectEntity updateEntity = ProjectEntity.builder()
                    .id(projectEntity.getId())
                    .name(projectDTO.getName())
                    .content(projectDTO.getContent())
                    .start(java.sql.Date.valueOf(startDate))
                    .end(java.sql.Date.valueOf(endDate))
                    .category(projectEntity.getCategory())
                    .cycle(projectDTO.getCycle())
                    .build();

            updateEntity = projectRepository.save(updateEntity);
            return updateEntity != null;  // 수정 성공 시 true, 실패 시 false 반환
        } else {
            return false; // 수정할 프로젝트가 존재하지 않는 경우
        }
    }

    /**
     * 프로젝트 삭제 함수
     *
     * @param pid 삭제할 프로젝트 아이디
     * @return 삭제 성공 여부 (true : 삭제 성공, false : 삭제할 프로젝트가 존재하지 않음)
     */
    public boolean deleteProject(Long pid) {
        // 프로젝트 아이디를 기반으로 프로젝트 조회 
        Optional<ProjectEntity> projectEntityOptional = projectRepository.findById(pid);

        if (projectEntityOptional.isPresent()) { // 조회한 엔티티 존재 여부 확인 

            // 해당 프로젝트의 멤버 데이터 조회
            List<MemberEntity> members = memberRepository.findByPid(pid);
            // 조회한 멤버 삭제
            memberRepository.deleteAll(members);

            projectRepository.delete(projectEntityOptional.get());
            return true; // 삭제 성공
        } else {
            return false; // 삭제할 프로젝트가 존재하지 않는 경우
        }
    }

    /**
     * 해당 프로젝트에 대한 멤버 리스트 정보 얻기 위한 함수
     *
     * @param pid 프로젝트 아이디
     * @return 프로젝트 멤버 정보를 담은 MemberDTO 객체 리스트
     */
    public List<MemberDTO> getMember(Long pid) {
        List<MemberEntity> memberEntities = memberRepository.findByPid(pid);
        List<MemberDTO> memberDTOs = new ArrayList<>();

        for (MemberEntity memberEntity : memberEntities) {
            MemberDTO memberDTO = MemberDTO.builder()
                    .mid(memberEntity.getMid())
                    .uid(memberEntity.getUid())
                    .pid(memberEntity.getPid())
                    .right(memberEntity.getRight())
                    .state(memberEntity.getState())
                    .build();

            memberDTOs.add(memberDTO);
        }
        return memberDTOs;
    }

    // 초대 받은 프로젝트 정보 
    public List<ProjectDTO> getInvitedProjects(String uid) {
        List<Long> invitedMembers = memberRepository.findPidByUidAndState(uid, 1);
        List<ProjectDTO> invitedProjects = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Long pid : invitedMembers) {
            Optional<ProjectEntity> projectEntityOptional = projectRepository.findById(pid);
            if (projectEntityOptional.isPresent()) {
                ProjectEntity projectEntity = projectEntityOptional.get();

                // 프로젝트의 시작 및 종료 기를 Instant 객체로 변환
                Instant startInstant = projectEntity.getStart().toInstant();
                Instant endInstant = projectEntity.getEnd().toInstant();
                // Instant 객체를 시스템 기본 시간대를 사용하여 LocalDate로 변환
                LocalDate startDate = startInstant.atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate endDate = endInstant.atZone(ZoneId.systemDefault()).toLocalDate();

                // ProjectDTO 객체 생성
                ProjectDTO projectDTO = ProjectDTO.builder()
                        .pid(projectEntity.getId())
                        .name(projectEntity.getName())
                        .content(projectEntity.getContent())
                        .start(startDate.format(formatter))
                        .end(endDate.format(formatter))
                        .category(projectEntity.getCategory())
                        .cycle(projectEntity.getCycle())
                        .build();

                // 카테고리 콤마로 나눠서 카테고리 리스트에 저장
//                String category = projectEntity.getCategory();
//                if (category != null && !category.isEmpty()) {
//                    String[] categories = category.split(",");
//                    List<String> categoryList = new ArrayList<>();
//                    for (String cat : categories) {
//                        categoryList.add(cat.trim());
//                    }
//                    projectDTO.setCategoryList(categoryList);
//                }

                invitedProjects.add(projectDTO);
            }
        }

        return invitedProjects;
    }

    // 프로젝트 초대한 사람 이름 
    public String getInviteUserName(Long pid) {
        String inviteUid = memberRepository.findUidByPidAndState(pid, 0);

        String inviteUserName = userRepository.findNameById(inviteUid);
        return inviteUserName;

    }

    // 초대받은 프로젝트 이름 
    public String getInviteName(Long pid) {
        String inviteName = projectRepository.findNameById(pid);
        return inviteName;
    }

    // 받은 초대 수락 
    public boolean acceptInvite(Long pid, String uid) {
        boolean invite = false;

        MemberEntity memberEntity = memberRepository.findByUidAndPid(uid, pid);
        if (memberEntity != null) {
            memberEntity.setState(2); // 상태를 2(수락)로 변경 
            memberRepository.save(memberEntity); // 변경 사항 저장
            invite = true;
        }

        return invite; // 하나 이상 수락된 경우 true 반환
    }

    // 받은 초대 거절 
    public boolean refuseInvite(Long pid, String uid) {
        boolean invite = false;

        MemberEntity memberEntity = memberRepository.findByUidAndPid(uid, pid);
        if (memberEntity != null) {
            memberEntity.setState(-1); // 상태를 -1(거절)로 변경 
            memberRepository.save(memberEntity); // 변경 사항 저장
            invite = true;
        }

        return invite;
    }

    /**
     * 추가할 멤버가 회원가입 된 아이디인지 판별
     *
     * @param uid 추가할 멤버 아이디
     * @return 회원가입 된 아이디인지 판별 여부 (true : 회원가입 된 아이디, false : 회원가입이 되지 않은 아이디)
     */
    public boolean isRegisteredUser(String uid) {
        return userRepository.existsById(uid);
    }

    /**
     * 해당 프로젝트에 이미 참여중인 멤버인지 판별
     *
     * @param pid 프로젝트 아이디
     * @param addUid 참여중인지 판별할 유저 아이디
     * @return 해당 프로젝트에 대한 참여 여부 (true : 이미 프로젝트에 참여 중인 아이디, false : 해당 프로젝트에 참여
     * 중이 아닌 아이디)
     */
    public boolean isMember(Long pid, String addUid) {
        return memberRepository.existsByUidAndPid(addUid, pid);
    }

    // 입력한 값이 들어간 아이디 찾기 
    public List<String> searchUsers(String uid, Long pid) {
        List<UserEntity> searchResults = userRepository.findByIdContainingIgnoreCase(uid);
        List<MemberEntity> memberList = memberRepository.findByPid(pid);
        List<String> uidList = new ArrayList<>();

        for (UserEntity user : searchResults) {
            boolean exits = false;
            if(user.getState() == 0){
                for (MemberEntity member : memberList) {
                    if (member.getUid().equals(user.getId())) {
                        exits = true;
                    }
                }
                if (!exits) {
                    uidList.add(user.getId());
                }
            }
        }
        return uidList;
    }

    // 프로젝트 내의 검색와 아이디가 비슷한 멤버를 찾는데 이미 list에 있는 id는 제외하고 찾기
    public List<UserDTO> searchMembers(Long pid, List<String> memberList) {
        if (memberList == null || memberList.isEmpty()) {
            memberList = Collections.singletonList("-1");  // 무효한 ID 값을 사용
        }
        List<Map<String, Object>> searchResults = userRepository.findUsersByPidAndUidList(pid, memberList);
        List<UserDTO> uidList = new ArrayList<>();
        for (Map<String, Object> user : searchResults) {
            uidList.add(UserDTO.builder()
                    .id(user.get("id").toString())
                    .name(user.get("name").toString())
                    .build());
        }
        return uidList;
    }

    public List<UserDTO> searchAllMembers(Long pid) {
        List<Map<String, Object>> searchResults = userRepository.findUsersByPid(pid);

        List<UserDTO> uidList = new ArrayList<>();

        for (Map<String, Object> user : searchResults) {
            uidList.add(UserDTO.builder()
                    .id(user.get("id").toString())
                    .name(user.get("name").toString())
                    .email(user.get("email").toString())
                    .birth(user.get("birth").toString())
                    .phone(user.get("phone").toString())
                    .build());
        }
        return uidList;
    }

    /**
     * 프로젝트 멤버 추가 함수
     *
     * @param addUid 추가할 멤버 아이디
     * @param pid 프로젝트 아이디
     * @return 맴보 추가 성공 여부 (true : 멤버 추가 성공, false : 멤버 추가 실패)
     */
    public boolean addMember(String addUid, Long pid) {

        // 이미 멤버인 경우 추가하지 않음
        if (isMember(pid, addUid)) {
            return false;
        }

        MemberEntity addEntity = MemberEntity.builder()
                .uid(addUid)
                .pid(pid)
                .right(2)
                .state(1)
                .build();

        addEntity = memberRepository.save(addEntity);

        return addEntity != null;
    }

    /**
     * 해당 프로젝트의 멤버 권한 수정 함수
     *
     * @param uid 권한 수정할 멤버 아이디
     * @param pid 프로젝트 아이디
     * @param right 수정할 권한
     * @return 권한 수정 여부 (true : 권한 수정 성공, false : 권한 수정 실패)
     */
    public boolean updateMemberRight(MemberDTO memberDTO, Long pid) {
        // 프로젝트 아이디와 유저 아이디를 기반으로 멤버 엔티티 조회
        MemberEntity memberEntity = memberRepository.findByUidAndPid(memberDTO.getUid(), pid);

        if (memberEntity != null) { // 멤버 엔티티가 존재하는 경우
            // 새로운 멤버 엔티티 생성
            MemberEntity updatedMember = MemberEntity.builder()
                    .mid(memberEntity.getMid())
                    .uid(memberDTO.getUid())
                    .pid(pid)
                    .right(memberDTO.getRight())
                    .state(memberDTO.getState())
                    .build();

            updatedMember = memberRepository.save(updatedMember);
            return updatedMember != null; // 권한 수정 성공 시 true, 실패 시 false 반환
        } else {
            return false; // 수정할 멤버가 존재하지 않는 경우
        }
    }

    /**
     * 프로젝트 멤버 삭제 함수
     *
     * @param selectedMember 선택된 삭제 멤버 아이디
     * @param pid 프로젝트 아이디
     * @return 삭제 성공 여부 (true : 삭제 성공, false : 삭제 실패, 선택된 삭제 멤버가 없음)
     */
    public boolean deleteMember(List<String> uids, Long pid) {
        boolean status = false; // 초기값은 false로 설정

        for (String uid : uids) {
            MemberEntity deleteMember = memberRepository.findByUidAndPid(uid, pid);
            if (deleteMember != null) {
                memberRepository.delete(deleteMember);
                status = true; // 하나 이상의 멤버를 삭제한 경우 true로 설정
            }
        }

        return status;
    }

    // 카테고리 목록 
    public List<String> getProjectCategory(Long pid) {
        String categories = projectRepository.findCategoryByProjectId(pid).get();

        if (categories != null && !categories.isEmpty()) {
            return Arrays.asList(categories.split(","));
        } else {
            return Collections.emptyList();  // 빈 리스트를 반환
        }
    }

    // 카테고리 삭제 
    public boolean deleteCategory(List<String> cats, Long pid) {
        boolean status = false;

        String categories = projectRepository.findCategoryByProjectId(pid).get();

        List<String> categoryList = new ArrayList<>(Arrays.asList(categories.split(",")));
        categoryList.removeAll(cats);

        // 수정된 카테고리를 콤마로 구분하여 문자열 생성 
        String updateCats = String.join(",", categoryList);

        projectRepository.updateCategory(pid, updateCats);
        projectRepository.updateCategoryToNotice(pid,cats);
        status = true;

        return status;
    }

    // 카테고리 추가 
    public boolean addCategory(String addCat, Long pid) {
        boolean status = false;

        String categories = projectRepository.findCategoryByProjectId(pid).get();

        String[] categoryArray = categories.split(",");
        for (String category : categoryArray) {
            if (category.equals(addCat)) {  // 카테고리 중복 
                return false;
            }
        }

        categories += "," + addCat;
        projectRepository.updateCategory(pid, categories);

        status = true;

        return status;
    }

    public UserDTO hasMember(Long pid, String uid) {
        Map<String, Object> user = userRepository.findUsersByPidAndUid(pid, uid);
        if (user == null) {
            return new UserDTO();
        }
        return UserDTO.builder()
                .id(user.get("id").toString())
                .name(user.get("name").toString())
                .build();
    }

    /**
     * 카테고리 업데이트
     *
     * @param str
     * @param pid
     */
    public void updateCartegory(String str, Long pid) {
        projectRepository.updateCategory(pid, str);
    }

    public int getDaysUntilProjectEnd(Long pid) {
        return projectRepository.getDaysUntilProjectEnd(pid);
    }
    
    /**
     * 자신이 초대된 프로젝트인지 확인하는 함수
     */
    public boolean checkInvite(Long pid, String uid){
        MemberEntity entity = memberRepository.findByPidAndUidAndRightNotNegative(pid, uid);
        return entity != null;
    }
            
}
