package com.seliote.mybatis4ii;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Action of create mapper, select a directory -> right click -> new -> MyBatis Mapper XML
 *
 * @author seliote
 * @since 2021-02-02
 */
public class MapperCreateAction extends AnAction {

    // Mapper XML file template
    private static final String MAPPER_TEMPLATE_PATH = "/template/mapper.xml";

    // Item is visible if this class in class path
    private static final String FLAG_ITEM_VISIBLE_CLASS = "org.apache.ibatis.session.Configuration";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        var input = dialogGetInput(project);
        if (input.isPresent()) {
            var fileName = input.get().endsWith(".xml") ?
                    input.get() : input.get() + ".xml";
            writeMapper(e, fileName);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        var project = e.getProject();
        var presentation = e.getPresentation();
        if (project == null) {
            presentation.setVisible(false);
            return;
        }
        var javaPsiFacade = JavaPsiFacade.getInstance(project);
        var psiClass = javaPsiFacade.findClass(
                FLAG_ITEM_VISIBLE_CLASS,
                GlobalSearchScope.allScope(project));
        presentation.setVisible(psiClass != null);
    }

    /**
     * Show dialog and get inout
     *
     * @param project Project object
     * @return User input
     */
    private Optional<String> dialogGetInput(Project project) {
        var inputDialog = new Messages.InputDialog(project,
                "MyBatis Mapper XML filename",
                "MyBatis Mapper XML",
                null,
                null,
                null);
        inputDialog.show();
        var input = inputDialog.getInputString();
        inputDialog.disposeIfNeeded();
        return Optional.ofNullable(input);
    }

    /**
     * Write file at context path
     *
     * @param anActionEvent AnActionEvent object
     * @param fileName      File name
     */
    private void writeMapper(AnActionEvent anActionEvent, String fileName) {
        Project project = anActionEvent.getProject();
        if (project == null) {
            Messages.showInfoMessage(
                    "Create Mapper XML failed, can not get Project",
                    "Mapper XML");
            return;
        }
        Runnable writeMapper = () -> {
            var virtualFile =
                    anActionEvent.getData(PlatformDataKeys.VIRTUAL_FILE);
            if (virtualFile == null) {
                Messages.showInfoMessage(
                        "Create Mapper XML failed, can not get VirtualFile",
                        "Mapper XML");
                return;
            }
            String content;
            Reader reader = new InputStreamReader(
                    MapperCreateAction.class.getResourceAsStream(MAPPER_TEMPLATE_PATH),
                    StandardCharsets.UTF_8);
            try (reader) {
                content = StreamUtil.readText(reader);
            } catch (IOException e) {
                Messages.showInfoMessage(
                        "Create Mapper XML failed, message: " + e.getMessage(),
                        "Mapper XML");
                return;
            }
            var file = (XmlFile) PsiFileFactory.getInstance(project)
                    .createFileFromText(fileName, XmlFileType.INSTANCE, content);
            var psiDirectory = PsiDirectoryFactory.getInstance(project)
                    .createDirectory(virtualFile);
            psiDirectory.add(file);
        };
        WriteCommandAction.runWriteCommandAction(project, writeMapper);
    }
}
