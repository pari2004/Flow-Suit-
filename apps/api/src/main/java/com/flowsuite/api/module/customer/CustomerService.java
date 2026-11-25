package com.flowsuite.api.module.customer;

import com.flowsuite.api.common.dto.PageResponse;
import com.flowsuite.api.common.error.ApiException;
import com.flowsuite.api.common.error.ErrorCode;
import com.flowsuite.api.domain.entity.Customer;
import com.flowsuite.api.domain.entity.Project;
import com.flowsuite.api.domain.enums.CustomerStatus;
import com.flowsuite.api.module.customer.dto.CreateCustomerRequest;
import com.flowsuite.api.module.customer.dto.CustomerResponse;
import com.flowsuite.api.module.customer.dto.UpdateCustomerRequest;
import com.flowsuite.api.repository.CustomerRepository;
import com.flowsuite.api.repository.OrganizationRepository;
import com.flowsuite.api.repository.ProjectRepository;
import com.flowsuite.api.repository.UserRepository;
import com.flowsuite.api.security.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Customer records CRUD + archive, tenant-scoped, optionally linked to a project (FR-30, FR-31). */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public CustomerService(CustomerRepository customerRepository,
                           ProjectRepository projectRepository,
                           OrganizationRepository organizationRepository,
                           UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.projectRepository = projectRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> list(UUID projectId, Pageable pageable) {
        UUID orgId = SecurityUtils.currentOrganizationId();
        var page = projectId == null
                ? customerRepository.findByOrganization_Id(orgId, pageable)
                : customerRepository.findByOrganization_IdAndProject_Id(orgId, projectId, pageable);
        return PageResponse.from(page, CustomerResponse::from);
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(UUID customerId) {
        return CustomerResponse.from(loadOwned(customerId));
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        UUID orgId = SecurityUtils.currentOrganizationId();
        Customer customer = new Customer();
        customer.setOrganization(organizationRepository.getReferenceById(orgId));
        customer.setName(request.name().trim());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setCompany(request.company());
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setProject(resolveProject(request.projectId(), orgId));
        customer.setCreatedBy(userRepository.getReferenceById(SecurityUtils.currentUserId()));
        customerRepository.save(customer);
        return CustomerResponse.from(customer);
    }

    @Transactional
    public CustomerResponse update(UUID customerId, UpdateCustomerRequest request) {
        UUID orgId = SecurityUtils.currentOrganizationId();
        Customer customer = loadOwned(customerId);
        customer.setName(request.name().trim());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setCompany(request.company());
        customer.setProject(resolveProject(request.projectId(), orgId));
        return CustomerResponse.from(customer);
    }

    @Transactional
    public CustomerResponse setArchived(UUID customerId, boolean archived) {
        Customer customer = loadOwned(customerId);
        customer.setStatus(archived ? CustomerStatus.ARCHIVED : CustomerStatus.ACTIVE);
        return CustomerResponse.from(customer);
    }

    @Transactional
    public void delete(UUID customerId) {
        customerRepository.delete(loadOwned(customerId));
    }

    // --- helpers ---

    /** Resolves and validates an optional project link within the same org. */
    private Project resolveProject(UUID projectId, UUID orgId) {
        if (projectId == null) {
            return null;
        }
        return projectRepository.findByIdAndOrganization_Id(projectId, orgId)
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST,
                        "Linked project not found in this organization."));
    }

    private Customer loadOwned(UUID customerId) {
        UUID orgId = SecurityUtils.currentOrganizationId();
        return customerRepository.findByIdAndOrganization_Id(customerId, orgId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Customer not found."));
    }
}
