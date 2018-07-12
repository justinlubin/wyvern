package wyvern.target.corewyvernIL.astvisitor;

import wyvern.stdlib.Globals;
import wyvern.target.corewyvernIL.Case;
import wyvern.target.corewyvernIL.FormalArg;
import wyvern.target.corewyvernIL.VarBinding;
import wyvern.target.corewyvernIL.decl.*;
import wyvern.target.corewyvernIL.decltype.*;
import wyvern.target.corewyvernIL.effects.Effect;
import wyvern.target.corewyvernIL.effects.EffectSet;
import wyvern.target.corewyvernIL.expression.*;
import wyvern.target.corewyvernIL.modules.Module;
import wyvern.target.corewyvernIL.support.ModuleResolver;
import wyvern.target.corewyvernIL.type.*;
import wyvern.tools.errors.HasLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EffectApproximationVisitor extends ASTVisitor<EffectApproximationState, Set<Effect>> {
    private static Set<Variable> importsFromSeqExpr(SeqExpr seqExpr) {
        Set<Variable> result = new HashSet<>();
        List<HasLocation> elements = seqExpr.getElements();
        int finalIndex = elements.size() - 1; // All but last element
        for (int i = 0; i < finalIndex; i++) {
            HasLocation element = elements.get(i);
            if (!(element instanceof VarBinding)) {
                continue;
            }
            IExpr body = ((VarBinding) element).getExpression();
            if (!(body instanceof Variable)) {
                continue;
            }
            Variable v = (Variable) body;
            String name = v.getName();
            if (name.length() < 4 || !name.substring(0, 4).equals("MOD$")) {
                continue;
            }
            result.add(v);
        }
        return result;
    }

    private static Set<Variable> imports(IExpr program) {
        if (program instanceof New) {
            List<Declaration> decls = ((New) program).getDecls();
            if (decls.size() == 0) {
                throw new RuntimeException("program has no decls: " + program);
            }
            Declaration firstDecl = decls.get(0);
            if (!(firstDecl instanceof DefDeclaration) || !firstDecl.getName().equals("apply")) {
                throw new RuntimeException("program is module functor without apply method: " + program);
            }
            IExpr body = ((DefDeclaration) firstDecl).getBody();
            if (!(body instanceof SeqExpr)) {
                throw new RuntimeException("body of program's apply method is not SeqExpr: " + program);
            }
            return importsFromSeqExpr((SeqExpr) body);
        } else if (program instanceof SeqExpr) {
            return importsFromSeqExpr((SeqExpr) program);
        } else {
            throw new RuntimeException("cannot get imports of program: " + program);
        }
    }

    public static Set<Effect> approximateEffectBound(ModuleResolver moduleResolver, Module module) {
        EffectApproximationVisitor visitor =
                new EffectApproximationVisitor();
        EffectApproximationState state =
                new EffectApproximationState(
                        moduleResolver,
                        Globals.getStandardTypeContext(),
                        module.getDependencies()
                );
        return approxModule(visitor, state, module);
    }

    // approx

    private static Set<Effect> approxModule(EffectApproximationVisitor visitor, EffectApproximationState state, Module module) {
        // TODO Check if annotated here

        Set<Effect> result = new HashSet<>();
        Set<Variable> programImports = imports(module.getExpression());
        for (Variable programImport : programImports) {
            result.addAll(approxModule(visitor, state, state.resolveModule(programImport)));
        }
        result.addAll(module.getExpression().getType().acceptVisitor(visitor, state));
        return result;
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, StructuralType structuralType) {
        Set<Effect> result = new HashSet<>();
        for (DeclType dt : structuralType.getDeclTypes()) {
            result.addAll(dt.acceptVisitor(this, state));
        }
        return result;
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, NominalType nominalType) {
        ValueType resolvedType = state.resolveNominalType(nominalType);
        if (resolvedType != null) {
            // Found in module dependencies
            return resolvedType.acceptVisitor(this, state);
        } else {
            ValueType vt = nominalType.getCanonicalType(state.getCachedStandardContext());
            if (vt != nominalType) {
                // Found in prelude
                return new HashSet<>(); // TODO; java has effects, for example
            } else {
                // Not found
                throw new RuntimeException("Cannot find nominal type " + nominalType);
            }
        }
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, ExtensibleTagType extensibleTagType) {
        throw new RuntimeException("EffectApproximationVisitor should not visit ExtensibleTagType");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, DataType dataType) {
        throw new RuntimeException("EffectApproximationVisitor should not visit DataType");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, ValueType valueType) {
        throw new RuntimeException("EffectApproximationVisitor should not visit ValueType");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, RefinementType refinementType) {
        throw new RuntimeException("EffectApproximationVisitor should not visit RefinementType");
    }

    // approxDecl

    @Override
    public Set<Effect> visit(EffectApproximationState state, AbstractTypeMember abstractTypeMember) {
        return new HashSet<>();
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, ConcreteTypeMember concreteTypeMember) {
        return new HashSet<>();
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, DefDeclType defDeclType) {
        Set<Effect> result = new HashSet<>();
        EffectSet producedEffects = defDeclType.getEffectSet();
        if (producedEffects != null) {
            // Annotated method
            result.addAll(producedEffects.getEffects());
        } else {
            // Unannotated method
            for (FormalArg arg : defDeclType.getFormalArgs()) {
                result.addAll(arg.getType().acceptVisitor(this, state));
            }
        }
        result.addAll(defDeclType.getRawResultType().acceptVisitor(this, state));
        return result;
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, EffectDeclType effectDeclType) {
        return new HashSet<>();
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, ValDeclType valDeclType) {
        return valDeclType.getRawResultType().acceptVisitor(this, state);
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, VarDeclType varDeclType) {
        return varDeclType.getRawResultType().acceptVisitor(this, state);
    }

    // End algorithm

    @Override
    public Set<Effect> visit(EffectApproximationState state, New newExpr) {
        throw new RuntimeException("EffectApproximationVisitor should not visit New");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, Case c) {
        throw new RuntimeException("EffectApproximationVisitor should not visit Case");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, MethodCall methodCall) {
        throw new RuntimeException("EffectApproximationVisitor should not visit MethodCall");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, Match match) {
        throw new RuntimeException("EffectApproximationVisitor should not visit Match");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, FieldGet fieldGet) {
        throw new RuntimeException("EffectApproximationVisitor should not visit FieldGet");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, Let let) {
        throw new RuntimeException("EffectApproximationVisitor should not visit Let");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, Bind bind) {
        throw new RuntimeException("EffectApproximationVisitor should not visit Bind");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, FieldSet fieldSet) {
        throw new RuntimeException("EffectApproximationVisitor should not visit FieldSet");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, Variable variable) {
        throw new RuntimeException("EffectApproximationVisitor should not visit Variable");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, Cast cast) {
        throw new RuntimeException("EffectApproximationVisitor should not visit Cast");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, VarDeclaration varDecl) {
        throw new RuntimeException("EffectApproximationVisitor should not visit VarDeclaration");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, DefDeclaration defDecl) {
        throw new RuntimeException("EffectApproximationVisitor should not visit DefDeclaration");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, ValDeclaration valDecl) {
        throw new RuntimeException("EffectApproximationVisitor should not visit ValDeclaration");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, ModuleDeclaration moduleDecl) {
        throw new RuntimeException("EffectApproximationVisitor should not visit ModuleDeclaration");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, IntegerLiteral integerLiteral) {
        throw new RuntimeException("EffectApproximationVisitor should not visit IntegerLiteral");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, BooleanLiteral booleanLiteral) {
        throw new RuntimeException("EffectApproximationVisitor should not visit BooleanLiteral");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, RationalLiteral rational) {
        throw new RuntimeException("EffectApproximationVisitor should not visit RationalLiteral");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, FormalArg formalArg) {
        throw new RuntimeException("EffectApproximationVisitor should not visit FormalArg");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, StringLiteral stringLiteral) {
        throw new RuntimeException("EffectApproximationVisitor should not visit StringLiteral");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, CharacterLiteral characterLiteral) {
        throw new RuntimeException("EffectApproximationVisitor should not visit CharacterLiteral");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, DelegateDeclaration delegateDecl) {
        throw new RuntimeException("EffectApproximationVisitor should not visit DelegateDeclaration");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, TypeDeclaration typeDecl) {
        throw new RuntimeException("EffectApproximationVisitor should not visit TypeDeclaration");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, FFIImport ffiImport) {
        throw new RuntimeException("EffectApproximationVisitor should not visit FFIImport");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, FFI ffi) {
        throw new RuntimeException("EffectApproximationVisitor should not visit FFI");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, EffectDeclaration effectDeclaration) {
        throw new RuntimeException("EffectApproximationVisitor should not visit EffectDeclaration");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, SeqExpr seqExpr) {
        throw new RuntimeException("EffectApproximationVisitor should not visit SeqExpr");
    }

    @Override
    public Set<Effect> visit(EffectApproximationState state, FloatLiteral flt) {
        throw new RuntimeException("EffectApproximationVisitor should not visit FloatLiteral");
    }
}
