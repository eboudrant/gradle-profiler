package org.gradle.profiler.mutations;

import org.gradle.profiler.BuildContext;

import java.io.File;

public class ApplyChangeToGraphQLSchemaFileMutator extends AbstractFileChangeMutator {

    public ApplyChangeToGraphQLSchemaFileMutator(File sourceFile) {
        super(sourceFile);
    }

    @Override
    protected void applyChangeTo(BuildContext context, StringBuilder text) {
        text.append("input ").append(context.getUniqueBuildId()).append(" {")
            .append("f: String")
            .append("}\n");
    }
}
