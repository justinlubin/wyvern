package wyvern.target.corewyvernIL.astvisitor;

import wyvern.stdlib.Globals;
import wyvern.target.corewyvernIL.Case;
import wyvern.target.corewyvernIL.FormalArg;
import wyvern.target.corewyvernIL.VarBinding;
import wyvern.target.corewyvernIL.decl.Declaration;
import wyvern.target.corewyvernIL.decl.DefDeclaration;
import wyvern.target.corewyvernIL.decl.DelegateDeclaration;
import wyvern.target.corewyvernIL.decl.EffectDeclaration;
import wyvern.target.corewyvernIL.decl.ModuleDeclaration;
import wyvern.target.corewyvernIL.decl.TypeDeclaration;
import wyvern.target.corewyvernIL.decl.ValDeclaration;
import wyvern.target.corewyvernIL.decl.VarDeclaration;
import wyvern.target.corewyvernIL.decltype.AbstractTypeMember;
import wyvern.target.corewyvernIL.decltype.ConcreteTypeMember;
import wyvern.target.corewyvernIL.decltype.DeclType;
import wyvern.target.corewyvernIL.decltype.DeclTypeWithResult;
import wyvern.target.corewyvernIL.decltype.DefDeclType;
import wyvern.target.corewyvernIL.decltype.EffectDeclType;
import wyvern.target.corewyvernIL.decltype.ValDeclType;
import wyvern.target.corewyvernIL.decltype.VarDeclType;
import wyvern.target.corewyvernIL.effects.Effect;
import wyvern.target.corewyvernIL.effects.EffectSet;
import wyvern.target.corewyvernIL.effects.TaggedEffect;
import wyvern.target.corewyvernIL.expression.Bind;
import wyvern.target.corewyvernIL.expression.BooleanLiteral;
import wyvern.target.corewyvernIL.expression.Cast;
import wyvern.target.corewyvernIL.expression.CharacterLiteral;
import wyvern.target.corewyvernIL.expression.Expression;
import wyvern.target.corewyvernIL.expression.FFI;
import wyvern.target.corewyvernIL.expression.FFIImport;
import wyvern.target.corewyvernIL.expression.FieldGet;
import wyvern.target.corewyvernIL.expression.FieldSet;
import wyvern.target.corewyvernIL.expression.FloatLiteral;
import wyvern.target.corewyvernIL.expression.IExpr;
import wyvern.target.corewyvernIL.expression.IntegerLiteral;
import wyvern.target.corewyvernIL.expression.Let;
import wyvern.target.corewyvernIL.expression.Match;
import wyvern.target.corewyvernIL.expression.MethodCall;
import wyvern.target.corewyvernIL.expression.New;
import wyvern.target.corewyvernIL.expression.RationalLiteral;
import wyvern.target.corewyvernIL.expression.SeqExpr;
import wyvern.target.corewyvernIL.expression.StringLiteral;
import wyvern.target.corewyvernIL.expression.Variable;
import wyvern.target.corewyvernIL.modules.Module;
import wyvern.target.corewyvernIL.modules.TypedModuleSpec;
import wyvern.target.corewyvernIL.support.ModuleResolver;
import wyvern.target.corewyvernIL.support.TypeContext;
import wyvern.target.corewyvernIL.type.DataType;
import wyvern.target.corewyvernIL.type.ExtensibleTagType;
import wyvern.target.corewyvernIL.type.NominalType;
import wyvern.target.corewyvernIL.type.RefinementType;
import wyvern.target.corewyvernIL.type.StructuralType;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.tools.errors.HasLocation;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class State {
    private ModuleResolver moduleResolver;
    private TypeContext cachedStandardContext;
    private Map<String, ValueType> nominalTypes;

    private ArrayDeque<String> breadcrumbs = new ArrayDeque<>();

    State(ModuleResolver moduleResolver, TypeContext standardContext, List<TypedModuleSpec> dependencies) {
        this.moduleResolver = moduleResolver;
        this.cachedStandardContext = standardContext;
        this.nominalTypes = new HashMap<>();
        for (TypedModuleSpec dep : dependencies) {
            this.nominalTypes.put(dep.getDefinedTypeName(), dep.getType());
        }
    }

    TypeContext getCachedStandardContext() {
        return this.cachedStandardContext;
    }

    ValueType resolveNominalType(NominalType nt) {
        String name = nt.getTypeMember();
        ValueType wrapper = nominalTypes.get(name);
        if (wrapper == null) {
            return null;
        }
        if (!(wrapper instanceof StructuralType)) {
            throw new RuntimeException("Cannot resolve nominal type: " + nt);
        }
        List<DeclType> dts = ((StructuralType) wrapper).getDeclTypes();
        if (dts.size() != 1) {
            throw new RuntimeException("Unexpected resolution of nominal type (length != 1): " + nt);
        }
        DeclType dt = dts.get(0);
        if (!(dt instanceof DeclTypeWithResult)) {
            throw new RuntimeException("Unexpected resolution of nominal type (not DeclTypeWithResult): " + nt);
        }
        return ((DeclTypeWithResult) dt).getRawResultType();
    }

    Module resolveModule(Variable v) {
        String name = v.getName();
        int dollarIndex = name.indexOf('$');
        String qualifiedName = name.substring(dollarIndex + 1);
        return this.moduleResolver.resolveModule(qualifiedName);
    }

    void addBreadcrumb(String s) {
        this.breadcrumbs.addFirst(s);
    }

    void resetBreadcrumbs(String s) {
        this.breadcrumbs.clear();
        this.breadcrumbs.addFirst(s);
    }

    ArrayDeque<String> getBreadcrumbs() {
        return this.breadcrumbs;
    }
}

public class EffectApproximationVisitor extends ASTVisitor<State, Set<TaggedEffect>> {
    // Entry point

    public static Set<TaggedEffect> approximateEffectBound(ModuleResolver moduleResolver, Module module) {
        EffectApproximationVisitor visitor =
                new EffectApproximationVisitor();
        State state =
                new State(
                        moduleResolver,
                        Globals.getStandardTypeContext(),
                        module.getDependencies()
                );
        return approxModule(visitor, state, module);
    }

    // Rule: annotated

    // Note: resource modules get translated into an object with an *unannotated* apply method, so this method will
    // return false for resource modules (even if they are actually annotated).
    private static boolean isAnnotated(ValueType t) {
        if (!(t instanceof StructuralType)) {
            return false;
        }
        List<DeclType> decls = ((StructuralType) t).getDeclTypes();
        for (DeclType decl : decls) {
            if (!(decl instanceof DefDeclType)) {
                continue;
            }
            EffectSet effectSet = ((DefDeclType) decl).getEffectSet();
            if (effectSet == null) {
                return false;
            }
        }
        return true;
    }

    // Rule: polyTy

    private static Set<TaggedEffect> polyTy(EffectApproximationVisitor visitor, State state, ValueType type) {
        if (type instanceof NominalType) {
            NominalType nt = (NominalType) type;
            if (nt.getPath() != null && nt.getPath().toString().startsWith("__generic__")) {
                return new HashSet<>();
            }
        }
        return type.acceptVisitor(visitor, state);
    }

    // Rule: polyFx

    private static Set<TaggedEffect> polyFx(EffectApproximationVisitor visitor, State state, EffectSet effectSet) {
        Set<TaggedEffect> result = new HashSet<>();
        for (Effect effect : effectSet.getEffects()) {
            if (effect.getPath() == null || !effect.getPath().toString().startsWith("__generic__")) {
                result.add(TaggedEffect.fromEffect(effect, state.getBreadcrumbs()));
            }
        }
        return result;
    }

    // Rule: imports

    private static Set<Variable> importsFromSeqExpr(SeqExpr seqExpr) {
        Set<Variable> result = new HashSet<>();
        for (HasLocation element : seqExpr.getElements()) {
            if (!(element instanceof VarBinding)) {
                continue;
            }
            IExpr body = ((VarBinding) element).getExpression();
            if (!(body instanceof Variable)) {
                continue;
            }
            Variable v = (Variable) body;
            String name = v.getName();
            if (!name.startsWith("MOD$")) {
                continue;
            }
            result.add(v);
        }
        return result;
    }

    private static Set<Variable> imports(IExpr program) {
        if (program instanceof New) {
            List<Declaration> decls = ((New) program).getDecls();
            if (decls.size() != 1) {
                throw new RuntimeException("Module functor has decls.size() != 1: " + program);
            }
            Declaration firstDecl = decls.get(0);
            if (!(firstDecl instanceof DefDeclaration) || !firstDecl.getName().equals("apply")) {
                throw new RuntimeException("Module functor has no apply method: " + program);
            }
            IExpr body = ((DefDeclaration) firstDecl).getBody();
            if (!(body instanceof SeqExpr)) {
                throw new RuntimeException("Module functor has apply method with non-SeqExpr body: " + program);
            }
            return importsFromSeqExpr((SeqExpr) body);
        } else if (program instanceof SeqExpr) {
            return importsFromSeqExpr((SeqExpr) program);
        } else {
            throw new RuntimeException("Cannot get imports of program: " + program);
        }
    }

    // Rule: approx

    private static Set<TaggedEffect> approxModule(EffectApproximationVisitor visitor, State state, Module module) {
        Expression expression = module.getExpression();
        ValueType type = expression.getType();

        Set<TaggedEffect> result = new HashSet<>();
        if (!isAnnotated(type)) {
            // Check the imports
            Set<Variable> programImports = imports(module.getExpression());
            for (Variable programImport : programImports) {
                Module importedModule = state.resolveModule(programImport);
                state.resetBreadcrumbs(importedModule.getSpec().getQualifiedName());
                result.addAll(approxModule(visitor, state, importedModule));
            }
        }

        state.resetBreadcrumbs(module.getSpec().getQualifiedName());
        result.addAll(type.acceptVisitor(visitor, state));
        return result;
    }

    @Override
    public Set<TaggedEffect> visit(State state, StructuralType structuralType) {
        Set<TaggedEffect> result = new HashSet<>();
        for (DeclType dt : structuralType.getDeclTypes()) {
            result.addAll(dt.acceptVisitor(this, state));
        }
        return result;
    }

    @Override
    public Set<TaggedEffect> visit(State state, NominalType nominalType) {
        ValueType resolvedType = state.resolveNominalType(nominalType);
        if (resolvedType != null) {
            // Found in module dependencies

            state.resetBreadcrumbs(nominalType.getTypeMember());
            return resolvedType.acceptVisitor(this, state);
        } else {
            ValueType vt = nominalType.getCanonicalType(state.getCachedStandardContext());
            if (vt != nominalType) {
                // Found in prelude
                return new HashSet<>(); // TODO java has effects, for example
            } else {
                // Not found
                throw new RuntimeException("Cannot find nominal type " + nominalType);
            }
        }
    }

    // Rule: approxDecl

    @Override
    public Set<TaggedEffect> visit(State state, AbstractTypeMember abstractTypeMember) {
        return new HashSet<>();
    }

    @Override
    public Set<TaggedEffect> visit(State state, ConcreteTypeMember concreteTypeMember) {
        return new HashSet<>();
    }

    @Override
    public Set<TaggedEffect> visit(State state, DefDeclType defDeclType) {
        Set<TaggedEffect> result = new HashSet<>();
        EffectSet producedEffects = defDeclType.getEffectSet();
        if (producedEffects != null) {
            // Annotated method
            result.addAll(polyFx(this, state, producedEffects));
        } else {
            // Unannotated method
            for (FormalArg arg : defDeclType.getFormalArgs()) {
                result.addAll(polyTy(this, state, arg.getType()));
            }
        }
        result.addAll(polyTy(this, state, defDeclType.getRawResultType()));
        return result;
    }

    @Override
    public Set<TaggedEffect> visit(State state, EffectDeclType effectDeclType) {
        return new HashSet<>();
    }

    @Override
    public Set<TaggedEffect> visit(State state, ValDeclType valDeclType) {
        state.addBreadcrumb(valDeclType.getName());
        return valDeclType.getRawResultType().acceptVisitor(this, state);
    }

    @Override
    public Set<TaggedEffect> visit(State state, VarDeclType varDeclType) {
        state.addBreadcrumb(varDeclType.getName());
        return varDeclType.getRawResultType().acceptVisitor(this, state);
    }

    // End algorithm

    @Override
    public Set<TaggedEffect> visit(State state, ExtensibleTagType extensibleTagType) {
        throw new RuntimeException("EffectApproximationVisitor should not visit ExtensibleTagType");
    }

    @Override
    public Set<TaggedEffect> visit(State state, DataType dataType) {
        throw new RuntimeException("EffectApproximationVisitor should not visit DataType");
    }

    @Override
    public Set<TaggedEffect> visit(State state, ValueType valueType) {
        throw new RuntimeException("EffectApproximationVisitor should not visit ValueType");
    }

    @Override
    public Set<TaggedEffect> visit(State state, RefinementType refinementType) {
        throw new RuntimeException("EffectApproximationVisitor should not visit RefinementType");
    }

    @Override
    public Set<TaggedEffect> visit(State state, New newExpr) {
        throw new RuntimeException("EffectApproximationVisitor should not visit New");
    }

    @Override
    public Set<TaggedEffect> visit(State state, Case c) {
        throw new RuntimeException("EffectApproximationVisitor should not visit Case");
    }

    @Override
    public Set<TaggedEffect> visit(State state, MethodCall methodCall) {
        throw new RuntimeException("EffectApproximationVisitor should not visit MethodCall");
    }

    @Override
    public Set<TaggedEffect> visit(State state, Match match) {
        throw new RuntimeException("EffectApproximationVisitor should not visit Match");
    }

    @Override
    public Set<TaggedEffect> visit(State state, FieldGet fieldGet) {
        throw new RuntimeException("EffectApproximationVisitor should not visit FieldGet");
    }

    @Override
    public Set<TaggedEffect> visit(State state, Let let) {
        throw new RuntimeException("EffectApproximationVisitor should not visit Let");
    }

    @Override
    public Set<TaggedEffect> visit(State state, Bind bind) {
        throw new RuntimeException("EffectApproximationVisitor should not visit Bind");
    }

    @Override
    public Set<TaggedEffect> visit(State state, FieldSet fieldSet) {
        throw new RuntimeException("EffectApproximationVisitor should not visit FieldSet");
    }

    @Override
    public Set<TaggedEffect> visit(State state, Variable variable) {
        throw new RuntimeException("EffectApproximationVisitor should not visit Variable");
    }

    @Override
    public Set<TaggedEffect> visit(State state, Cast cast) {
        throw new RuntimeException("EffectApproximationVisitor should not visit Cast");
    }

    @Override
    public Set<TaggedEffect> visit(State state, VarDeclaration varDecl) {
        throw new RuntimeException("EffectApproximationVisitor should not visit VarDeclaration");
    }

    @Override
    public Set<TaggedEffect> visit(State state, DefDeclaration defDecl) {
        throw new RuntimeException("EffectApproximationVisitor should not visit DefDeclaration");
    }

    @Override
    public Set<TaggedEffect> visit(State state, ValDeclaration valDecl) {
        throw new RuntimeException("EffectApproximationVisitor should not visit ValDeclaration");
    }

    @Override
    public Set<TaggedEffect> visit(State state, ModuleDeclaration moduleDecl) {
        throw new RuntimeException("EffectApproximationVisitor should not visit ModuleDeclaration");
    }

    @Override
    public Set<TaggedEffect> visit(State state, IntegerLiteral integerLiteral) {
        throw new RuntimeException("EffectApproximationVisitor should not visit IntegerLiteral");
    }

    @Override
    public Set<TaggedEffect> visit(State state, BooleanLiteral booleanLiteral) {
        throw new RuntimeException("EffectApproximationVisitor should not visit BooleanLiteral");
    }

    @Override
    public Set<TaggedEffect> visit(State state, RationalLiteral rational) {
        throw new RuntimeException("EffectApproximationVisitor should not visit RationalLiteral");
    }

    @Override
    public Set<TaggedEffect> visit(State state, FormalArg formalArg) {
        throw new RuntimeException("EffectApproximationVisitor should not visit FormalArg");
    }

    @Override
    public Set<TaggedEffect> visit(State state, StringLiteral stringLiteral) {
        throw new RuntimeException("EffectApproximationVisitor should not visit StringLiteral");
    }

    @Override
    public Set<TaggedEffect> visit(State state, CharacterLiteral characterLiteral) {
        throw new RuntimeException("EffectApproximationVisitor should not visit CharacterLiteral");
    }

    @Override
    public Set<TaggedEffect> visit(State state, DelegateDeclaration delegateDecl) {
        throw new RuntimeException("EffectApproximationVisitor should not visit DelegateDeclaration");
    }

    @Override
    public Set<TaggedEffect> visit(State state, TypeDeclaration typeDecl) {
        throw new RuntimeException("EffectApproximationVisitor should not visit TypeDeclaration");
    }

    @Override
    public Set<TaggedEffect> visit(State state, FFIImport ffiImport) {
        throw new RuntimeException("EffectApproximationVisitor should not visit FFIImport");
    }

    @Override
    public Set<TaggedEffect> visit(State state, FFI ffi) {
        throw new RuntimeException("EffectApproximationVisitor should not visit FFI");
    }

    @Override
    public Set<TaggedEffect> visit(State state, EffectDeclaration effectDeclaration) {
        throw new RuntimeException("EffectApproximationVisitor should not visit EffectDeclaration");
    }

    @Override
    public Set<TaggedEffect> visit(State state, SeqExpr seqExpr) {
        throw new RuntimeException("EffectApproximationVisitor should not visit SeqExpr");
    }

    @Override
    public Set<TaggedEffect> visit(State state, FloatLiteral flt) {
        throw new RuntimeException("EffectApproximationVisitor should not visit FloatLiteral");
    }
}
