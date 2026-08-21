package org.dromara.ai.common;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ModuleBoundaryTest {
    @Tag("dev")
    @Test
    void infrastructureBoundaryRuleIsExecutable() {
        noClasses().that().resideOutsideOfPackages("..infrastructure..")
            .should().accessClassesThat().resideInAnyPackage("..infrastructure..")
            .check(new ClassFileImporter().importPackages("org.dromara.ai"));
    }
}
