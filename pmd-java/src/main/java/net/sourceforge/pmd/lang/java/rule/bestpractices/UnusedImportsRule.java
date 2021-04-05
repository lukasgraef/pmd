/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.rule.bestpractices;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceType;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.java.ast.ASTImportDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTName;
import net.sourceforge.pmd.lang.java.ast.ASTPackageDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTPrimaryExpression;
import net.sourceforge.pmd.lang.java.ast.ASTPrimaryPrefix;
import net.sourceforge.pmd.lang.java.ast.ASTPrimarySuffix;
import net.sourceforge.pmd.lang.java.ast.Comment;
import net.sourceforge.pmd.lang.java.ast.FormalComment;
import net.sourceforge.pmd.lang.java.ast.TypeNode;
import net.sourceforge.pmd.lang.java.ast.internal.ImportWrapper;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;

public class UnusedImportsRule extends AbstractJavaRule {

    protected Set<ImportWrapper> imports = new HashSet<>();

    /*
     * Patterns to match the following constructs:
     *
     * @see package.class#member(param, param) label {@linkplain
     * package.class#member(param, param) label} {@link
     * package.class#member(param, param) label} {@link package.class#field}
     * {@value package.class#field}
     *
     * @throws package.class label
     */
    private static final Pattern SEE_PATTERN = Pattern
            .compile("@see\\s+((?:\\p{Alpha}\\w*\\.)*(?:\\p{Alpha}\\w*))?(?:#\\w*(?:\\(([.\\w\\s,\\[\\]]*)\\))?)?");

    private static final Pattern LINK_PATTERNS = Pattern
            .compile("\\{@link(?:plain)?\\s+((?:\\p{Alpha}\\w*\\.)*(?:\\p{Alpha}\\w*))?(?:#\\w*(?:\\(([.\\w\\s,\\[\\]]*)\\))?)?[\\s\\}]");

    private static final Pattern VALUE_PATTERN = Pattern.compile("\\{@value\\s+(\\p{Alpha}\\w*)[\\s#\\}]");

    private static final Pattern THROWS_PATTERN = Pattern.compile("@throws\\s+(\\p{Alpha}\\w*)");

    private static final Pattern[] PATTERNS = { SEE_PATTERN, LINK_PATTERNS, VALUE_PATTERN, THROWS_PATTERN };

    @Override
    public Object visit(ASTCompilationUnit node, Object data) {
        imports.clear();
        super.visit(node, data);
        visitComments(node);

        /*
         * special handling for Bug 2606609 : False "UnusedImports" positive in
         * package-info.java package annotations are processed before the import
         * clauses so they need to be examined again later on.
         */
        if (node.getNumChildren() > 0 && node.getChild(0) instanceof ASTPackageDeclaration) {
            visit((ASTPackageDeclaration) node.getChild(0), data);
        }
        for (ImportWrapper wrapper : imports) {
            addViolation(data, wrapper.getNode(), wrapper.getFullName());
        }
        return data;
    }

    private void visitComments(ASTCompilationUnit node) {
        if (imports.isEmpty()) {
            return;
        }
        for (Comment comment : node.getComments()) {
            if (!(comment instanceof FormalComment)) {
                continue;
            }
            for (Pattern p : PATTERNS) {
                Matcher m = p.matcher(comment.getImage());
                while (m.find()) {
                    String fullname = m.group(1);

                    if (fullname != null) { // may be null for "@see #" and "@link #"
                        removeReferenceSingleImport(fullname);
                    }

                    if (m.groupCount() > 1) {
                        fullname = m.group(2);
                        if (fullname != null) {
                            for (String param : fullname.split("\\s*,\\s*")) {
                                removeReferenceSingleImport(param);
                            }
                        }
                    }

                    if (imports.isEmpty()) {
                        return;
                    }
                }
            }
        }
    }

    @Override
    public Object visit(ASTImportDeclaration node, Object data) {
        imports.add(new ImportWrapper(node));
        return data;
    }

    @Override
    public Object visit(ASTClassOrInterfaceType node, Object data) {
        check(node);
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTName node, Object data) {
        check(node);
        return data;
    }

    protected void check(Node node) {
        if (imports.isEmpty()) {
            return;
        }
        Pair<String, String> candidate = getImportWrapper(node);
        String candFullName = candidate.getLeft();
        String candName = candidate.getRight();

        // check exact imports
        Iterator<ImportWrapper> it = imports.iterator();
        while (it.hasNext()) {
            ImportWrapper i = it.next();
            if (!i.isStaticOnDemand() && i.matches(candFullName, candName)) {
                it.remove();
                return;
            }
        }

        // check static on-demand imports
        it = imports.iterator();
        while (it.hasNext()) {
            ImportWrapper i = it.next();
            if (i.isStaticOnDemand() && i.matches(candFullName, candName)) {
                it.remove();
                return;
            }
        }

        if (node instanceof TypeNode && ((TypeNode) node).getType() != null) {
            Class<?> c = ((TypeNode) node).getType();
            if (c.getPackage() != null) {
                removeOnDemand(c.getPackage().getName());
            }
        }
    }



    protected Pair<String, String> getImportWrapper(Node node) {
        String fullName = node.getImage();
        String name;
        if (!isQualifiedName(node)) {
            name = node.getImage();
        } else {
            // ASTName could be: MyClass.MyConstant
            // name -> MyClass
            // fullName -> MyClass.MyConstant
            name = node.getImage().substring(0, node.getImage().indexOf('.'));
            if (isMethodCall(node)) {
                // ASTName could be: MyClass.MyConstant.method(a, b)
                // name -> MyClass
                // fullName -> MyClass.MyConstant
                fullName = node.getImage().substring(0, node.getImage().lastIndexOf('.'));
            }
        }

        return Pair.of(fullName, name);
    }

    private boolean isMethodCall(Node node) {
        // PrimaryExpression
        //     PrimaryPrefix
        //         Name
        //     PrimarySuffix

        if (node.getParent() instanceof ASTPrimaryPrefix && node.getNthParent(2) instanceof ASTPrimaryExpression) {
            Node primaryPrefix = node.getParent();
            Node expression = primaryPrefix.getParent();

            boolean hasNextSibling = expression.getNumChildren() > primaryPrefix.getIndexInParent() + 1;
            if (hasNextSibling) {
                Node nextSibling = expression.getChild(primaryPrefix.getIndexInParent() + 1);
                if (nextSibling instanceof ASTPrimarySuffix) {
                    return true;
                }
            }
        }
        return false;
    }

    /** We found a reference to the type given by the name. */
    private void removeReferenceSingleImport(String referenceName) {
        int firstDot = referenceName.indexOf('.');
        String expectedImport = firstDot < 0 ? referenceName : referenceName.substring(0, firstDot);
        for (Iterator<ImportWrapper> iterator = imports.iterator(); iterator.hasNext(); ) {
            ImportWrapper anImport = iterator.next();
            if (!anImport.isOnDemand() && anImport.getName().equals(expectedImport)) {
                iterator.remove();
            }
        }
    }

    private void removeOnDemand(String fullName) {
        for (Iterator<ImportWrapper> iterator = imports.iterator(); iterator.hasNext(); ) {
            ImportWrapper anImport = iterator.next();
            if (anImport.isOnDemand() && anImport.getFullName().equals(fullName)) {
                iterator.remove();
                break;
            }
        }
    }
}
