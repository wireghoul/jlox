package com.craftinginterpreters.lox;

class Interpreter implements Expr.Visitor<Object> {


    @Override public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                return -(double)right;
        }

        // Unreachable.
        return null;
    }

    @Override public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right); 

        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperand(expr.operator, left);
                checkNumberOperand(expr.operator, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperand(expr.operator, left);
                checkNumberOperand(expr.operator, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperand(expr.operator, left);
                checkNumberOperand(expr.operator, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperand(expr.operator, left);
                checkNumberOperand(expr.operator, right);
                return (double)left <= (double)right;
            case MINUS:
                checkNumberOperand(expr.operator, left);
                checkNumberOperand(expr.operator, right);
                return (double)left - (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                } 
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                break;
            case SLASH:
                checkNumberOperand(expr.operator, left);
                checkNumberOperand(expr.operator, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperand(expr.operator, left);
                checkNumberOperand(expr.operator, right);
                return (double)left * (double)right;
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
        }

        // Unreachable.
        return null;
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null) {
            return false;
        }
        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) { 
            return;
        }
        throw new RuntimeError(operator, "Operand must be a number.");
    }
}