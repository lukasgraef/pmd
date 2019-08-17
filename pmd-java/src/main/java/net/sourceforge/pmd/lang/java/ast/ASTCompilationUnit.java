/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.ast;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import net.sourceforge.pmd.annotation.InternalApi;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.ast.RootNode;
import net.sourceforge.pmd.lang.java.typeresolution.ClassTypeResolver;

// FUTURE Change this class to extend from SimpleJavaNode, as TypeNode is not appropriate (unless I'm wrong)
public class ASTCompilationUnit extends AbstractJavaTypeNode implements RootNode {

    private ClassTypeResolver classTypeResolver;
    private List<Comment> comments;
    private Map<Integer, String> noPmdComments = Collections.emptyMap();

    @InternalApi
    @Deprecated
    public ASTCompilationUnit(int id) {
        super(id);
    }

    @InternalApi
    @Deprecated
    public ASTCompilationUnit(JavaParser p, int id) {
        super(p, id);
    }

    public List<Comment> getComments() {
        return comments;
    }

    @InternalApi
    @Deprecated
    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    @Override
    public Object jjtAccept(JavaParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }


    @Override
    public <T> void jjtAccept(SideEffectingVisitor<T> visitor, T data) {
        visitor.visit(this, data);
    }


    public boolean declarationsAreInDefaultPackage() {
        return getPackageDeclaration() == null;
    }

    public ASTPackageDeclaration getPackageDeclaration() {
        if (jjtGetNumChildren() > 0) {
            Node n = jjtGetChild(0);
            return n instanceof ASTPackageDeclaration ? (ASTPackageDeclaration) n : null;
        }
        return null;
    }

    @InternalApi
    @Deprecated
    public ClassTypeResolver getClassTypeResolver() {
        return classTypeResolver;
    }

    @InternalApi
    @Deprecated
    public void setClassTypeResolver(ClassTypeResolver classTypeResolver) {
        this.classTypeResolver = classTypeResolver;
    }

    Map<Integer, String> getNoPmdComments() {
        return noPmdComments;
    }

    void setNoPmdComments(Map<Integer, String> noPmdComments) {
        this.noPmdComments = noPmdComments;
    }
}
