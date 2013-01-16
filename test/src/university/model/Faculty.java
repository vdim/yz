package university.model;

import java.util.HashSet;

public class Faculty extends Person {
    private String office;
    private HashSet<Course> courses = new HashSet<Course>();
    private HashSet<Course> coursesCreated = new HashSet<Course>();
    private Company company;
    
    public String getOffice() {
	return office;
    }
    
    public void setOffice(String office) {
	this.office = office;
    }
    
    public HashSet<Course> getCourses() {
	return courses;
    }

    public void addCourse(Course s) {
	courses.add(s);
    }

    public HashSet<Course> getCoursesCreated() {
	return coursesCreated;
    }

    public void addCourseCreated(Course s) {
	coursesCreated.add(s);
    }

    @Override
    public String toString() {
        String name = getName();
       
        if (name == null || name.length() == 0)
            return "";
        
        return name;
    }
    
    public Company getCompany() {
	return company;
    }
    
    public void setCompany(Company company) {
	this.company = company;
    }
    
}
