package wyvern.target.corewyvernIL.astvisitor;

import wyvern.target.corewyvernIL.decltype.DeclType;
import wyvern.target.corewyvernIL.decltype.DeclTypeWithResult;
import wyvern.target.corewyvernIL.modules.TypedModuleSpec;
import wyvern.target.corewyvernIL.type.NominalType;
import wyvern.target.corewyvernIL.type.StructuralType;
import wyvern.target.corewyvernIL.type.ValueType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EffectApproximationState {
    private Map<String, ValueType> nominalTypes;

    public EffectApproximationState(List<TypedModuleSpec> dependencies) {
        this.nominalTypes = new HashMap<>();
        for (TypedModuleSpec dep : dependencies) {
            this.nominalTypes.put(dep.getDefinedTypeName(), dep.getType());
        }
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
}
