package com.onedayoffer.taskdistribution.services;

import com.onedayoffer.taskdistribution.DTO.EmployeeDTO;
import com.onedayoffer.taskdistribution.DTO.TaskDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class TaskDistributorImpl implements TaskDistributor {

    public static final int MAX_WORKING_TIME = 7 * 60;

    /**
     * Суть алгоритма: взять работника, который освободился раньше всех и (если позволяет время) выдать задачу ему
     * Примерная сложность алгоритма с учётом использования TreeMap:
     * O(n * log(k)), где n - количество заданий, k - сотрудников
     *
     * @param employees список работников для которых нужно выделить задачи
     * @param tasks список задач
     */
    @Override
    public void distribute(List<EmployeeDTO> employees, List<TaskDTO> tasks) {
        
        /*
        * Map для работников: ключом являет время окончания предыдущей задачи каждого работника
        * 
        * То есть если по ключу 5 лежат Петя и Вася, то это значит что Петя и Вася закончили выполнять
        * свои предыдущие задачи в 5 минут
        * 
        * TreeMap сортирует ключи по-возрастанию !!!!
        */
        SortedMap<Integer, List<EmployeeDTO>> employeeMap = new TreeMap<>();
        
        // заполняем Map всеми работниками по ключу 0 - так как до этого задач не было
        employees.forEach(employee -> putValueByKey(employeeMap, 0, employee));

        // сортируем с помощью компаратора (читать описание компаратора)
        tasks.sort(getComparator());

        tasks.forEach(task -> {
            // с помощью firstKey берём работника, который освободился раньше всех
            var startTime = employeeMap.firstKey();
            // высчитываем время, когда освободится работник если возьмёт эту задачу
            var endTime = startTime + task.getLeadTime();
            if (endTime > MAX_WORKING_TIME) {
                // работник будет занят слишком долго
                // всех других работников рассматривать нет смысла, так как они освободились также или позже - пропускаем
                log.warn("Задача {} не будет взята в работу", task);
            } else {
                // задача нам подходит - берём первого попавшего работника, закончившего в startTime (в дальнейшем можно предусмотреть приоритет работников)
                var selectedEmployee = removeAnyValueByKey(employeeMap, startTime);
                // выдаём задачу
                selectedEmployee.getTasks().add(task);
                // кладём сотрудника в Map по новому времени освобождения
                putValueByKey(employeeMap, endTime, selectedEmployee);
            }
        });

        // по моему алгоритму: если какой-то сотрудник остался недозагруженным - значит мало задач
    }

    /**
     * Кладёт в map значение, с учётом того что нужен список
     */
    private void putValueByKey(Map<Integer, List<EmployeeDTO>> map, Integer key, EmployeeDTO employee) {
        map.putIfAbsent(key, new ArrayList<>());
        map.get(key).add(employee);
    }

    private EmployeeDTO removeAnyValueByKey(SortedMap<Integer, List<EmployeeDTO>> employeeMap, Integer startTime) {
        var listEmp = employeeMap.get(startTime);
        var selectedEmployee = listEmp.remove(0);
        // если после вытаскивания работника - в это время не заканчивал не один другой сотрудник - удаляем ключ из Map
        if (listEmp.isEmpty()) {
            employeeMap.remove(startTime);
        }
        return selectedEmployee;
    }

    /**
     * Сортируем в первую очередь по приоритетам (сначала 1 потом 10)
     * При равенстве приоритетов берём в первую очередь длинные по времени задачи
     *
     * @return компаратор для сортировки
     */
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
