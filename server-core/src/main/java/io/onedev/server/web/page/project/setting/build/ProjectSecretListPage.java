package io.onedev.server.web.page.project.setting.build;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.feedback.FencedFeedbackPanel;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.ProjectManager;
import io.onedev.server.model.support.JobSecret;
import io.onedev.server.util.SecurityUtils;
import io.onedev.server.util.inputspec.SecretInput;
import io.onedev.server.web.editable.PropertyContext;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.page.admin.user.buildsetting.UserSecretListPage;
import io.onedev.server.web.page.my.buildsetting.MySecretListPage;

@SuppressWarnings("serial")
public class ProjectSecretListPage extends ProjectBuildSettingPage {

	public ProjectSecretListPage(PageParameters params) {
		super(params);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		String note = String.format("Define secrets to be used in build jobs. Secret value less "
				+ "than %d characters will not be masked in build log", SecretInput.MASK.length());
		add(new Label("note", note).setEscapeModelStrings(false));
		
		Form<?> form = new Form<Void>("form") {

			@Override
			protected void onSubmit() {
				super.onSubmit();
				
				Set<String> names = new HashSet<>();
				for (JobSecret secret: getProject().getBuildSetting().getSecrets()) {
					if (names.contains(secret.getName())) {
						error("Duplicate name found: " + secret.getName());
						return;
					} else {
						names.add(secret.getName());
					}
				}
				
				OneDev.getInstance(ProjectManager.class).save(getProject());
				getSession().success("Job secrets have been saved");
				setResponsePage(ProjectSecretListPage.class, ProjectSecretListPage.paramsOf(getProject()));
			}
			
		};
		
		form.add(new FencedFeedbackPanel("feedback", form));
		
		form.add(PropertyContext.editModel("editor", new AbstractReadOnlyModel<Serializable>() {

			@Override
			public Serializable getObject() {
				return getProject().getBuildSetting();
			}
			
		}, "secrets"));
		
		add(form);
		
		List<JobSecret> inheritedSecrets = getProject().getBuildSetting().getInheritedSecrets(getProject());
		InheritedSecretsBean bean = new InheritedSecretsBean();
		bean.setSecrets(inheritedSecrets);
		add(PropertyContext.view("inheritedSecrets", bean, "secrets"));
		
		if (SecurityUtils.isAdministrator()) {
			add(new BookmarkablePageLink<Void>("owner", UserSecretListPage.class, 
					UserSecretListPage.paramsOf(getProject().getOwner())));
		} else if (getProject().getOwner().equals(SecurityUtils.getUser())) {
			add(new BookmarkablePageLink<Void>("owner", MySecretListPage.class)); 
		} else {
			add(new WebMarkupContainer("owner") {

				@Override
				protected void onComponentTag(ComponentTag tag) {
					super.onComponentTag(tag);
					tag.setName("span");
				}
				
			});
		}
	}

	@Editable
	public static class InheritedSecretsBean implements Serializable {
		
		private List<JobSecret> secrets;

		@Editable
		public List<JobSecret> getSecrets() {
			return secrets;
		}

		public void setSecrets(List<JobSecret> secrets) {
			this.secrets = secrets;
		}
		
	}
}