package com.onedayoffer.taskdistribution.services;

import com.onedayoffer.taskdistribution.DTO.EmployeeDTO;
import com.onedayoffer.taskdistribution.DTO.TaskDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class TaskDistributorImpl implements TaskDistributor {

    @Override
    public void distribute(List<EmployeeDTO> employees, List<TaskDTO> tasks) {
        SortedMap<Integer, List<EmployeeDTO>> map = new TreeMap<>();
        employees.forEach(emp -> {
            map.putIfAbsent(0, new ArrayList<>());
            map.get(0).add(emp);
        });

        tasks.sort(getComparator());

        tasks.forEach(task -> {
            var startTime = map.firstKey();
            var endTime = startTime + task.getLeadTime();
            if (endTime > 7 * 60) {
                log.warn("Задача {} не будет взята в работу", task);
            } else {
                var listEmp = map.get(startTime);
                var emp = listEmp.remove(0);
                if (listEmp.isEmpty()) {
                    map.remove(startTime);
                }
                emp.getTasks().add(task);
                map.putIfAbsent(endTime, new ArrayList<>());
                map.get(endTime).add(emp);
            }
        });
    }

    private Comparator<TaskDTO> getComparator() {
        return (t1, t2) -> {
            var priority1 = t1.getPriority();
            var priority2 = t2.getPriority();
            if (!Objects.equals(priority1, priority2)) {
                return priority1.compareTo(priority2);
            }
            var time1 = t1.getLeadTime();
            var time2 = t2.getLeadTime();
            return time2.compareTo(time1);
        };
    }
}
