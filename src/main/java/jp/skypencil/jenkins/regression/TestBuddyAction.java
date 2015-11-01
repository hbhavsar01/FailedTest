package jp.skypencil.jenkins.regression;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;

import hudson.ExtensionPoint;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.Run;
import hudson.tasks.junit.CaseResult;
import hudson.util.RunList;
import jp.skypencil.jenkins.regression.TestBuddyHelper;

public class TestBuddyAction extends Actionable implements Action {
	@SuppressWarnings("rawtypes")
	AbstractProject project;

	
	public TestBuddyAction(@SuppressWarnings("rawtypes") AbstractProject project){
		this.project = project;
	}

	/**
     * The display name for the action.
     * 
     * @return the name as String
     */
    public final String getDisplayName() {
        return "Test Buddy";
    }

    /**
     * The icon for this action.
     * 
     * @return the icon file as String
     */
    public final String getIconFileName() {
    	return "/images/jenkins.png";
    }

    /**
     * The url for this action.
     * 
     * @return the url as String
     */
    public String getUrlName() {
        return "test_buddy";
    }

    /**
     * Search url for this action.
     * 
     * @return the url as String
     */
	public String getSearchUrl() {
		return "test_buddy";
	}

    @SuppressWarnings("rawtypes")
	public AbstractProject getProject(){
    	return this.project;
    }

	public String getUrl() {
		return this.project.getUrl() + this.getUrlName();
	}
	
	public String getUrlParam(String parameterName) {
		return Stapler.getCurrentRequest().getParameter(parameterName);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public List<BuildInfo> getBuilds() {
		List<BuildInfo> builds = new ArrayList<BuildInfo>();
		RunList<Run> runs = project.getBuilds();
		Iterator<Run> runIterator = runs.iterator();
		while (runIterator.hasNext()) {
			Run run = runIterator.next();
			BuildInfo build = new BuildInfo(run.getNumber(), run.getTimestamp(), run.getTimestampString2());
			builds.add(build);
		}
		
		return builds;
	}
	
	@SuppressWarnings("rawtypes")
	public BuildInfo getBuildInfo(String number) {
		int buildNumber = Integer.parseInt(number);
		Run run = project.getBuildByNumber(buildNumber);
		return new BuildInfo(run.getNumber(), run.getTimestamp(), run.getTimestampString2());
	}
	
	@SuppressWarnings("rawtypes")
	public List<TestInfo> getTests(String number) {
		List<TestInfo> tests = new ArrayList<TestInfo>();
		
		int buildNumber = Integer.parseInt(number);
		AbstractBuild build = project.getBuildByNumber(buildNumber);
		
		ArrayList<CaseResult> caseResults = TestBuddyHelper.getAllCaseResultsForBuild(build);
		for (CaseResult caseResult : caseResults) {
			String className = "";
			String[] fullClassName = caseResult.getClassName().split("\\.");
			if (fullClassName.length > 0) {
				className = fullClassName[fullClassName.length - 1];
			}
			String name[] = caseResult.getDisplayName().split("\\.");
			String short_name = name[name.length-1];
			if(caseResult.isFailed()){
				tests.add(new TestInfo(short_name, className, caseResult.getPackageName(), "Failed"));
			}
			else if(caseResult.isPassed()){
				tests.add(new TestInfo(short_name, className, caseResult.getPackageName(), "Passed"));
			}
			else if(caseResult.isSkipped()){
				tests.add(new TestInfo(short_name, className, caseResult.getPackageName(), "Skipped"));
			}			
		}
		
		return tests;
	}
	
	public List<TestInfo> getNewPassFail() {
		AbstractBuild build = project.getLastBuild();
		List<TestInfo> failPassTests = new ArrayList<TestInfo>();
		ArrayList<CaseResult> newlyFailPass = TestBuddyHelper.getChangedTestsBetweenBuilds(build, build.getPreviousBuild());
		for (CaseResult caseResult : newlyFailPass) {
			if(caseResult.isFailed()){
				failPassTests.add(new TestInfo(caseResult.getDisplayName(), null, caseResult.getPackageName(), "Newly Failed"));
			}
			else{
				failPassTests.add(new TestInfo(caseResult.getDisplayName(), null, caseResult.getPackageName(), "Newly Passed"));
			}		
		}
		//System.out.println("size of newPassFail array is " + newlyFailPass.size());
		return failPassTests;
	}
	
	
	public static class BuildInfo implements ExtensionPoint {
		private int number;
		private Calendar timestamp;
		private String timestampString;

		@DataBoundConstructor
		public BuildInfo(int number, Calendar timestamp, String timestampString) {
			this.number = number;
			this.timestamp = timestamp;
			this.timestampString = timestampString;
		}

		public int getNumber() {
			return number;
		}

		public String getTimestampString() {
			return timestampString;
		}

		public String getReadableTimestamp() {
			SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy h:mm:ss a");
			return dateFormat.format(timestamp.getTime());
		}
	}
	
	public static class TestInfo implements ExtensionPoint {
		private String name;
		private String className;
		private String packageName;
		private String status;
		
		@DataBoundConstructor
		public TestInfo(String name, String className, String packageName, String status) {
			this.name = name;
			this.className = className;
			this.packageName = packageName;
			this.status = status;
		}
		
		public String getName() {
			return name;
		}

		public String getClassName() {
			return className;
		}
		
		public String getPackageName() {
			return packageName;
		}
		
		public String getStatus() {
			return status;
		}
	}
	
}