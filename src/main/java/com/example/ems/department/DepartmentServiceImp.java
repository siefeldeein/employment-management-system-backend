package com.example.ems.department;

import com.example.ems.department.dto.DepartmentCreateReq;
import com.example.ems.department.dto.DepartmentDetailsResponse;
import com.example.ems.department.dto.DepartmentResponse;
import com.example.ems.department.dto.DepartmentUpdateReq;
import com.example.ems.common.exception.DuplicateResourceException;
import com.example.ems.common.exception.InvalidInputException;
import com.example.ems.common.exception.ResourceNotFoundException;
import com.example.ems.department.mapper.DepartmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentServiceImp implements DepartmentService {

    // 1️⃣ DEPENDENCIES (top of class)
    final private DepartmentMapper departmentMapper;
    final private DepartmentRepository departmentRepository;

    // 2️⃣ CREATE METHODS
    @Override
    public DepartmentResponse createDepartment(DepartmentCreateReq createReq) {

        if (departmentRepository.existsByNameIgnoreCase(createReq.getName())) {
            throw new DuplicateResourceException("Department name '" + createReq.getName() + "' already exists");
        }

        Department department = departmentMapper.toEntity(createReq);
        Department savedDepartment = departmentRepository.save(department);
        return departmentMapper.toDtoResponse(savedDepartment);
    }

    // 3️⃣ READ METHODS GROUP
    @Override
    public DepartmentResponse getDepartmentById(Long id) {

        Department returnedDepartment =
        departmentRepository.findById(id).orElseThrow(
        ()-> new ResourceNotFoundException("This Department Id "+ id + "doesn't exists"));

        return departmentMapper.toDtoResponse(returnedDepartment);
    }

    @Override
    public DepartmentDetailsResponse getDepartmentByIdWithEmployees(Long id) {

        Department department = departmentRepository.findWithEmployeesById(id).orElseThrow(
        ()-> new ResourceNotFoundException("This Department Id "+ id + "doesn't exists"));

        return departmentMapper.toDtoResponseDetails(department);
    }

    @Override
    public DepartmentResponse getDepartmentByName(String name) {

        Department returnedDepartment = departmentRepository.findByNameIgnoreCase(name).orElseThrow(
        ()-> new ResourceNotFoundException("This Department name "+ name + "doesn't exists"));

        return departmentMapper.toDtoResponse(returnedDepartment);
    }

    @Override
    public List<DepartmentResponse> getAllDepartments() {

        List<Department> departmentList = departmentRepository.findAll();

        return departmentList.stream()
            .map(departmentMapper::toDtoResponse)
            .collect(Collectors.toList());
    }

    @Override
    public Page<DepartmentResponse> getAllDepartments(Pageable pageable) {

        Page<Department> departments = departmentRepository.findAll(pageable);
        return departments.map(departmentMapper::toDtoResponse);
    }

    // 4️⃣ UPDATE METHODS
    @Override
    public DepartmentResponse updateDepartment(Long id, DepartmentUpdateReq updateReq) {

       Department department = departmentRepository.findById(id).orElseThrow(
       ()-> new ResourceNotFoundException("This department with id " + id + " does not exist"));

       // helper method to validate the update
       validateDepartmentUpdate(updateReq, department);

       //convert the dto to the entity and update in dB
       departmentMapper.updateDepartment(updateReq, department);
       return departmentMapper.toDtoResponse(department);
    }

    // 5️⃣ DELETE METHODS
    public void deleteDepartment(Long id){

        Department department = departmentRepository.findById(id)
            .orElseThrow(()-> new ResourceNotFoundException("Department not found with id " + id));

        if(!department.getEmployeeList().isEmpty()){
            throw new InvalidInputException("Cannot delete department with assigned employees");
        }

        departmentRepository.delete(department);
    }


    // helper methods
    private void validateDepartmentUpdate(DepartmentUpdateReq updateReq, Department department) {

        if (updateReq.getName() != null)
        {
            //check if they left the name empty
            if (updateReq.getName().trim().isEmpty()) {
                throw new InvalidInputException("Department name cannot be empty");
            }

            //check first if they left the name unchanged (false) then no need to check uniqueness
            //if they changed the name, We need to check if other departments hold the same name
            if(!department.getName().equalsIgnoreCase(updateReq.getName()) &&
            departmentRepository.existsByNameIgnoreCase(updateReq.getName())) {

                throw new DuplicateResourceException("Department name '" + updateReq.getName() + "' already exists");
            }
        }

    }
}
