package wyvern.target.corewyvernIL.astvisitor;

import wyvern.target.corewyvernIL.decltype.DeclType;
import wyvern.target.corewyvernIL.decltype.DeclTypeWithResult;
import wyvern.target.corewyvernIL.expression.Variable;
import wyvern.target.corewyvernIL.modules.Module;
import wyvern.target.corewyvernIL.modules.TypedModuleSpec;
import wyvern.target.corewyvernIL.support.ModuleResolver;
import wyvern.target.corewyvernIL.support.TypeContext;
import wyvern.target.corewyvernIL.type.NominalType;
import wyvern.target.corewyvernIL.type.StructuralType;
import wyvern.target.corewyvernIL.type.ValueType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EffectApproximationState {
    private ModuleResolver moduleResolver;
    private TypeContext cachedStandardContext;
    private Map<String, ValueType> nominalTypes;

    public EffectApproximationState(ModuleResolver moduleResolver, TypeContext standardContext, List<TypedModuleSpec> dependencies) {
        this.moduleResolver = moduleResolver;
        this.cachedStandardContext = standardContext;
        this.nominalTypes = new HashMap<>();
        for (TypedModuleSpec dep : dependencies) {
            this.nominalTypes.put(dep.getDefinedTypeName(), dep.getType());
        }
    }

    public TypeContext getCachedStandardContext() {
        return this.cachedStandardContext;
    }

    public ValueType resolveNominalType(NominalType nt) {
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

    public Module resolveModule(Variable v) {
        String name = v.getName();
        int dollarIndex = name.indexOf('$');
        String qualifiedName = name.substring(dollarIndex + 1);
        return this.moduleResolver.resolveModule(qualifiedName);
    }
}
