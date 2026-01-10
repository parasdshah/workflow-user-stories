package com.workflow.hrms.config;

import com.workflow.hrms.entity.*;
import com.workflow.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class HrmsDataSeeder implements CommandLineRunner {

    private final RefRegionRepository regionRepo;
    private final RefBusinessSegmentRepository segmentRepo;
    private final RefProductRepository productRepo;
    private final RoleMasterRepository roleRepo;
    private final EmployeeMasterRepository employeeRepo;
    private final EmployeeMatrixAssignmentRepository matrixRepo;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (regionRepo.count() > 0) {
            System.out.println("HRMS Data already seeded. Skipping.");
            return;
        }

        System.out.println("Seeding HRMS Data...");

        // 1. Regions (Hierarchy)
        // Root
        RefRegion global = createRegion("Global", RefRegion.RegionType.GLOBAL, null, "/1/");
        
        // Continents
        RefRegion apac = createRegion("APAC", RefRegion.RegionType.CONTINENT, global, global.getPath());
        RefRegion emea = createRegion("EMEA", RefRegion.RegionType.CONTINENT, global, global.getPath());
        RefRegion na = createRegion("NA", RefRegion.RegionType.CONTINENT, global, global.getPath());

        // Countries
        RefRegion india = createRegion("India", RefRegion.RegionType.COUNTRY, apac, apac.getPath());
        RefRegion uk = createRegion("UK", RefRegion.RegionType.COUNTRY, emea, emea.getPath());
        RefRegion usa = createRegion("USA", RefRegion.RegionType.COUNTRY, na, na.getPath());

        // Cities
        RefRegion mumbai = createRegion("Mumbai", RefRegion.RegionType.CITY, india, india.getPath());
        RefRegion delhi = createRegion("Delhi", RefRegion.RegionType.CITY, india, india.getPath());
        RefRegion london = createRegion("London", RefRegion.RegionType.CITY, uk, uk.getPath());
        RefRegion ny = createRegion("New York", RefRegion.RegionType.CITY, usa, usa.getPath());

        // Branches (3 per city)
        List<RefRegion> branches = new ArrayList<>();
        branches.add(createBranch("Nariman Point", mumbai, "400021"));
        branches.add(createBranch("Bandra", mumbai, "400050"));
        branches.add(createBranch("Andheri", mumbai, "400053"));
        branches.add(createBranch("CP", delhi, "110001"));
        branches.add(createBranch("Saket", delhi, "110017"));
        branches.add(createBranch("Canary Wharf", london, "E14"));
        branches.add(createBranch("Manhattan", ny, "10001"));
        
        // 2. Segments & Products
        RefBusinessSegment retail = createSegment("Retail Banking", null);
        RefBusinessSegment corporate = createSegment("Corporate Banking", null);

        RefProduct homeLoan = createProduct("Home Loan", retail);
        RefProduct personalLoan = createProduct("Personal Loan", retail);
        RefProduct workingCapital = createProduct("Working Capital", corporate);

        // 3. Roles
        RoleMaster rClerk = createRole("CLERK", "Clerk", 0, "USD");
        RoleMaster rOfficer = createRole("OFFICER", "Officer", 10000, "USD");
        RoleMaster rManager = createRole("MANAGER", "Manager", 50000, "USD");
        RoleMaster rRegionalHead = createRole("REGIONAL_HEAD", "Regional Head", 1000000, "USD");

        // 4. Employees (50 Users) & Matrix Assignments
        Random random = new Random();
        String[] firstNames = {"Alice", "Bob", "Charlie", "David", "Eve", "Frank", "Grace", "Hank", "Ivy", "Jack"};
        
        List<EmployeeMaster> allEmployees = new ArrayList<>();

        // Create 5 Leadership Users (Fixed)
        createEmployeeWithAssignment("EMP001", "Global CEO", global, rRegionalHead, global, null, null, new BigDecimal("10000000"), "USD");
        createEmployeeWithAssignment("EMP002", "APAC Head", apac, rRegionalHead, apac, null, null, new BigDecimal("5000000"), "USD");
        createEmployeeWithAssignment("EMP003", "India Head", india, rRegionalHead, india, null, null, new BigDecimal("2000000"), "INR"); // Local Currency
        createEmployeeWithAssignment("EMP004", "UK Head", uk, rRegionalHead, uk, null, null, new BigDecimal("2000000"), "GBP");
        createEmployeeWithAssignment("EMP005", "USA Head", usa, rRegionalHead, usa, null, null, new BigDecimal("3000000"), "USD");

        // Create 45 Branch Users
        for (int i = 6; i <= 50; i++) {
            String empId = String.format("EMP%03d", i);
            String name = firstNames[i % 10] + " " + (i / 10 + 1); // e.g., Alice 2
            RefRegion branch = branches.get(random.nextInt(branches.size())); // Random branch
            
            // Random Role distribution
            RoleMaster role;
            BigDecimal limit;
            int roleType = random.nextInt(3);
            if (roleType == 0) { role = rClerk; limit = BigDecimal.ZERO; }
            else if (roleType == 1) { role = rOfficer; limit = new BigDecimal("20000"); }
            else { role = rManager; limit = new BigDecimal("100000"); }

            createEmployeeWithAssignment(empId, name, branch, role, branch, retail, homeLoan, limit, "INR");
        }

        System.out.println("HRMS Seeding Completed.");
    }

    private RefRegion createRegion(String name, RefRegion.RegionType type, RefRegion parent, String parentPath) {
        RefRegion r = new RefRegion();
        r.setRegionName(name);
        r.setRegionType(type);
        r.setParentRegion(parent);
        RefRegion saved = regionRepo.save(r);
        
        // Calculate Path
        String myPath = (parent == null ? "/" : parentPath) + saved.getRegionId() + "/";
        saved.setPath(myPath);
        return regionRepo.save(saved);
    }

    private RefRegion createBranch(String name, RefRegion parent, String pincode) {
        RefRegion r = new RefRegion();
        r.setRegionName(name);
        r.setRegionType(RefRegion.RegionType.BRANCH);
        r.setParentRegion(parent);
        r.setPincode(pincode);
        RefRegion saved = regionRepo.save(r);
        saved.setPath(parent.getPath() + saved.getRegionId() + "/");
        return regionRepo.save(saved);
    }

    private RefBusinessSegment createSegment(String name, RefBusinessSegment parent) {
        RefBusinessSegment s = new RefBusinessSegment();
        s.setSegmentName(name);
        s.setParentSegment(parent);
        return segmentRepo.save(s);
    }

    private RefProduct createProduct(String name, RefBusinessSegment segment) {
        RefProduct p = new RefProduct();
        p.setProductName(name);
        p.setSegment(segment);
        return productRepo.save(p);
    }

    private RoleMaster createRole(String code, String name, int limit, String currency) {
        RoleMaster r = new RoleMaster();
        r.setRoleCode(code);
        r.setRoleName(name);
        r.setBaseAuthorityLimit(new BigDecimal(limit));
        r.setBaseCurrency(currency);
        return roleRepo.save(r);
    }

    private void createEmployeeWithAssignment(String id, String name, RefRegion baseLocation, 
                                            RoleMaster role, RefRegion scopeRegion, 
                                            RefBusinessSegment scopeSegment, RefProduct scopeProduct,
                                            BigDecimal limit, String currency) {
        EmployeeMaster e = new EmployeeMaster();
        e.setEmployeeId(id);
        e.setFullName(name);
        e.setEmail(id.toLowerCase() + "@bank.com");
        e.setStatus(EmployeeMaster.EmployeeStatus.ACTIVE);
        e.setBaseLocation(baseLocation);
        employeeRepo.save(e);

        EmployeeMatrixAssignment m = new EmployeeMatrixAssignment();
        m.setEmployee(e);
        m.setRole(role);
        m.setScopeRegion(scopeRegion);
        m.setScopeSegment(scopeSegment);
        m.setScopeProduct(scopeProduct);
        m.setApprovalLimit(limit);
        m.setCurrencyCode(currency);
        m.setDenomination("ACTUALS");
        matrixRepo.save(m);
    }
}
