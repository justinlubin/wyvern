package wyvern.tools.typedAST.core.declarations;

import java.util.List;

import wyvern.target.corewyvernIL.decltype.DeclType;
import wyvern.target.corewyvernIL.modules.TypedModuleSpec;
import wyvern.target.corewyvernIL.support.GenContext;
import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.ToolError;
import wyvern.tools.typedAST.abs.Declaration;
import wyvern.tools.typedAST.core.expressions.Variable;
import wyvern.tools.typedAST.interfaces.CoreAST;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.types.Type;

public class DelegateDeclaration extends Declaration implements CoreAST {
    private TypedAST target;
    private Type type;
    private FileLocation location;

    public DelegateDeclaration(Type type, TypedAST target, FileLocation location) {
        this.type = type;
        this.target = target;
        this.location = location;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public FileLocation getLocation() {
        return this.location;
    }

    @Override
    public String getName() {
        return "aDelegation";
    }

    @Override
    public DeclType genILType(GenContext ctx) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public wyvern.target.corewyvernIL.decl.Declaration generateDecl(GenContext ctx, GenContext thisContext) {
        if (!(target instanceof Variable)) {
            ToolError.reportError(ErrorMessage.DELEGATE_MUST_BE_VARIABLE,
                    this,
                    target.toString());
        }
        String targetName = ((Variable) target).getName();
        wyvern.target.corewyvernIL.decl.DelegateDeclaration iLDelegateDecl
            = new wyvern.target.corewyvernIL.decl.DelegateDeclaration(type.getILType(ctx), targetName, location);
        return iLDelegateDecl;
    }

    @Override
    public wyvern.target.corewyvernIL.decl.Declaration topLevelGen(GenContext ctx, List<TypedModuleSpec> dependencies) {
        // TODO Auto-generated method stub
        return null;
    }

}
