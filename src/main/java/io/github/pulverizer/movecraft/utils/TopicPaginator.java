package io.github.pulverizer.movecraft.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TopicPaginator {
    private String title;
    private List<String> lines = new ArrayList<>();

    public TopicPaginator(String title){
        this.title = title;
    }

    public boolean addLine(String line){
        boolean result = lines.add(line);
        Collections.sort(lines);
        return result;
    }

    /**
     * Page numbers begin at 1
     * @param pageNumber
     * @return An array of lines to send as a page
     */
    public String[] getPage(int pageNumber){
        if(!isInBounds(pageNumber))
            throw new IndexOutOfBoundsException("Page number " + pageNumber + " exceeds bounds <" + 1 + "," + getPageCount() + ">");
        String[] tempLines = new String[pageNumber == getPageCount() ? (lines.size()%CLOSED_CHAT_PAGE_HEIGHT) + 1 : CLOSED_CHAT_PAGE_HEIGHT];
        tempLines[0] = "§e§l--- §r§6" + title +" §e§l-- §r§6page §c" + pageNumber + "§e/§c" + getPageCount() + " §e§l---";
        for(int i = 0; i< tempLines.length-1; i++)
            tempLines[i+1] = lines.get(((CLOSED_CHAT_PAGE_HEIGHT-1) * (pageNumber-1)) + i);
        return tempLines;
    }

    public int getPageCount(){
        return (int)Math.ceil(((double)lines.size())/(CLOSED_CHAT_PAGE_HEIGHT-1));
    }

    public boolean isInBounds(int pageNumber){
        return pageNumber > 0 && pageNumber <= getPageCount();
    }

    public boolean isEmpty(){
        return lines.isEmpty();
    }
}
