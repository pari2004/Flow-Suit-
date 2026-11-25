package com.flowsuite.api.module.customer;

import com.flowsuite.api.common.dto.PageResponse;
import com.flowsuite.api.module.customer.dto.CreateCustomerRequest;
import com.flowsuite.api.module.customer.dto.CustomerResponse;
import com.flowsuite.api.module.customer.dto.UpdateCustomerRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Customer records are managed by Owners/Admins/Managers (FR-30). */
@RestController
@RequestMapping("/api/v1/customers")
@PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER')")
@Tag(name = "Customers", description = "Customer record management")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @Operation(summary = "List customers; optional projectId filter")
    public PageResponse<CustomerResponse> list(@RequestParam(required = false) UUID projectId,
                                               @PageableDefault(size = 20) Pageable pageable) {
        return customerService.list(projectId, pageable);
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Get a customer")
    public CustomerResponse get(@PathVariable UUID customerId) {
        return customerService.get(customerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a customer")
    public CustomerResponse create(@Valid @RequestBody CreateCustomerRequest request) {
        return customerService.create(request);
    }

    @PutMapping("/{customerId}")
    @Operation(summary = "Update a customer")
    public CustomerResponse update(@PathVariable UUID customerId,
                                   @Valid @RequestBody UpdateCustomerRequest request) {
        return customerService.update(customerId, request);
    }

    @PatchMapping("/{customerId}/archive")
    @Operation(summary = "Archive a customer")
    public CustomerResponse archive(@PathVariable UUID customerId) {
        return customerService.setArchived(customerId, true);
    }

    @PatchMapping("/{customerId}/restore")
    @Operation(summary = "Restore an archived customer")
    public CustomerResponse restore(@PathVariable UUID customerId) {
        return customerService.setArchived(customerId, false);
    }

    @DeleteMapping("/{customerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a customer")
    public void delete(@PathVariable UUID customerId) {
        customerService.delete(customerId);
    }
}
