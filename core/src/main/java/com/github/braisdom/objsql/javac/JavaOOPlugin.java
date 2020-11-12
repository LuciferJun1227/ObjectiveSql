package com.github.braisdom.objsql.javac;

import com.github.braisdom.objsql.apt.APTBuilder;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.*;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;
import org.mangosdk.spi.ProviderFor;

@ProviderFor(Plugin.class)
public class JavaOOPlugin implements Plugin {

    public static final String NAME = "JavaOO";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init(JavacTask task, String... args) {
        BasicJavacTask javacTask = (BasicJavacTask) task;
        Context context = javacTask.getContext();
        TreeMaker treeMaker = TreeMaker.instance(context);
        Names names = Names.instance(context);

        APTBuilder aptBuilder = new APTBuilder(treeMaker, names);

        task.addTaskListener(new TaskListener() {
            @Override
            public void started(TaskEvent e) {
            }

            @Override
            public void finished(TaskEvent e) {
                if (e.getKind() != TaskEvent.Kind.PARSE) {
                    return;
                }
                e.getCompilationUnit()
                        .accept(new TreeScanner<Void, Void>() {
                            @Override
                            public Void visitVariable(VariableTree node, Void unused) {
                                JCVariableDecl variableDecl = (JCVariableDecl) node;
                                if (variableDecl.init != null && variableDecl.init instanceof JCBinary) {
                                    JCExpression expression = JCBinarys.createOperatorExpr(aptBuilder,
                                            (JCBinary) variableDecl.init);
                                    variableDecl.init = expression;
                                }

                                return super.visitVariable(node, unused);
                            }

                            @Override
                            public Void visitReturn(ReturnTree node, Void unused) {
                                JCReturn jcReturn = (JCReturn) node;
                                if(jcReturn.expr instanceof JCBinary) {
                                    JCExpression expression = JCBinarys.createOperatorExpr(aptBuilder,
                                            (JCBinary) jcReturn.expr);
                                    jcReturn.expr = expression;
                                }
                                return super.visitReturn(node, unused);
                            }

                            @Override
                            public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                                ListBuffer<JCExpression> newArgs = new ListBuffer<>();
                                List<JCExpression> args = (List<JCExpression>) node.getArguments();
                                for (ExpressionTree arg : args) {
                                    if (arg instanceof JCBinary) {
                                        JCExpression expression = JCBinarys.createOperatorExpr(aptBuilder,
                                                (JCBinary) arg);
                                        newArgs = newArgs.append(expression);
                                    } else
                                        newArgs = newArgs.append((JCExpression) arg);
                                }

                                JCMethodInvocation methodInvocation = (JCMethodInvocation) node;
                                methodInvocation.args = newArgs.toList();

                                return super.visitMethodInvocation(node, unused);
                            }
                        }, null);
            }
        });
    }
}