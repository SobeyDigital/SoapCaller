package com.sobey.jcg.sobeyhive.wsinvoker.lang2.parse;

/**
 * Created by WX on 2015/10/12.
 */
public class Result{
    private long id;
    private String templateCode;
    private String templateName;

    public Result(){

        }

    public long getId() {
        return id;
        }

    public void setId(long id) {
        this.id = id;
        }

    public String getTemplateName() {
        return templateName;
        }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
        }

    public String getTemplateCode() {
        return templateCode;
        }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
        }
}