# API Endpoints

| Tag | Method | Path | Auth | Summary | Request | Response | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Auth | POST | /login | No | User login | query: ; path: ; body: LoginBody | Login result (R<LoginData>) | Response follows R<T>; success code=200, failure code!=200. |
| Auth | POST | /register | No | User register | query: ; path: ; body: SysUser | Register result (R<Void>) | Response follows R<T>; success code=200, failure code!=200. |
| SystemUser | GET | /system/user/list | No | List users (paged) | query: pageNum,pageSize,username,realName,status; path: ; body: - | R<Page<SysUser>> | Pagination response data fields: current, size, total, records. |
| SystemUser | GET | /system/user/{userId} | No | Get user detail | query: ; path: userId; body: - | R<SysUser> | Response follows R<T>; success code=200, failure code!=200. |
| SystemUser | DELETE | /system/user/{userId} | No | Delete users by CSV ids | query: ; path: userIds; body: - | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| SystemUser | POST | /system/user | No | Create user | query: ; path: ; body: SysUser | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| SystemUser | PUT | /system/user | No | Update user | query: ; path: ; body: SysUser | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| SystemRole | GET | /system/role/list | No | List roles (paged) | query: pageNum,pageSize,roleName; path: ; body: - | R<Page<SysRole>> | Pagination response data fields: current, size, total, records. |
| SystemRole | POST | /system/role | No | Create role | query: ; path: ; body: SysRole | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| SystemRole | PUT | /system/role | No | Update role | query: ; path: ; body: SysRole | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| SystemRole | DELETE | /system/role/{roleIds} | No | Delete roles by CSV ids | query: ; path: roleIds; body: - | R<Void> (success or business-fail in body code) | Role delete pre-check: if sys_user_role bindings > 0, deletion forbidden (code != 200). |
| SystemMenu | GET | /system/menu/list | No | List menus | query: ; path: ; body: - | R<List<SysMenu>> | Response follows R<T>; success code=200, failure code!=200. |
| SystemMenu | POST | /system/menu | No | Create menu | query: ; path: ; body: SysMenu | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| SystemMenu | PUT | /system/menu | No | Update menu | query: ; path: ; body: SysMenu | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| SystemMenu | DELETE | /system/menu/{menuId} | No | Delete menu | query: ; path: menuId; body: - | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| CropVariety | GET | /crop/variety/list | No | List crop varieties (paged) | query: pageNum,pageSize,cropName; path: ; body: - | R<Page<BaseCropVariety>> | Pagination response data fields: current, size, total, records. |
| CropVariety | GET | /crop/variety/all | No | List all crop varieties | query: ; path: ; body: - | R<List<BaseCropVariety>> | Response follows R<T>; success code=200, failure code!=200. |
| CropVariety | POST | /crop/variety | No | Create crop variety | query: ; path: ; body: BaseCropVariety | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| CropVariety | PUT | /crop/variety | No | Update crop variety | query: ; path: ; body: BaseCropVariety | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| CropVariety | DELETE | /crop/variety/{ids} | No | Delete crop varieties by CSV ids | query: ; path: ids; body: - | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| CropBatch | GET | /crop/batch/list | No | List crop batches (paged) | query: pageNum,pageSize,plotId,currentStage; path: ; body: - | R<Page<CropBatch>> | Pagination response data fields: current, size, total, records. |
| CropBatch | POST | /crop/batch | No | Create crop batch | query: ; path: ; body: CropBatch | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| CropBatch | PUT | /crop/batch | No | Update crop batch | query: ; path: ; body: CropBatch | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| CropBatch | DELETE | /crop/batch/{ids} | No | Delete crop batches by CSV ids | query: ; path: ids; body: - | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| GrowthLog | GET | /crop/growth-log/list/{batchId} | No | List growth logs by batch id | query: ; path: batchId; body: - | R<List<GrowthStageLog>> | Response follows R<T>; success code=200, failure code!=200. |
| GrowthLog | POST | /crop/growth-log | No | Create growth log | query: ; path: ; body: GrowthStageLog | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| IotRule | GET | /iot/rule/list | No | List IoT task rules (paged) | query: pageNum,pageSize; path: ; body: - | R<Page<AgriTaskRule>> | Pagination response data fields: current, size, total, records. |
| IotRule | POST | /iot/rule | No | Create IoT task rule | query: ; path: ; body: AgriTaskRule | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| IotRule | PUT | /iot/rule | No | Update IoT task rule | query: ; path: ; body: AgriTaskRule | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| IotRule | DELETE | /iot/rule/{id} | No | Delete IoT task rule | query: ; path: id; body: - | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| IotData | GET | /iot/data/list | No | List IoT sensor data (paged) | query: pageNum,pageSize,plotId,sensorType; path: ; body: - | R<Page<IotSensorData>> | Pagination response data fields: current, size, total, records. |
| Task | GET | /task/list | No | List tasks (paged) | query: pageNum,pageSize,taskName,status,executorId,assigneeId; path: ; body: - | R<Page<AgriTask>> | Pagination response data fields: current, size, total, records. |
| Task | POST | /task | No | Create task (status forced to 0) | query: ; path: ; body: AgriTaskCreate | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| Task | PUT | /task | No | Update task basic info only | query: ; path: ; body: TaskUpdateDTO | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| Task | PUT | /task/assign | Yes (ADMIN, FARM_OWNER) | Assign task (ADMIN/FARM_OWNER, 0 -> 1) | query: ; path: ; body: TaskAssignDTO | R<Void> | State transition: 0 -> 1 (pending_assign to pending_accept). Assignee must be active FARMER. |
| Task | POST | /task/accept | Yes (FARMER) | Accept task (FARMER assignee only, 1 -> 2) | query: ; path: ; body: TaskAcceptDTO | R<Void> | State transition: 1 -> 2 (pending_accept to accepted). Caller must be the assigned farmer. |
| Task | POST | /task/reject | Yes (FARMER) | Reject task (FARMER assignee only, 1 -> 0) | query: ; path: ; body: TaskRejectDTO | R<Void> | State transition: 1 -> 0 (pending_accept to pending_assign). Caller must be the assigned farmer. |
| Task | DELETE | /task/{ids} | No | Delete tasks by CSV ids | query: ; path: ids; body: - | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| TaskLog | GET | /task/log/list | No | List task execution logs (paged) | query: pageNum,pageSize,taskId; path: ; body: - | R<Page<TaskExecutionLog>> | Pagination response data fields: current, size, total, records. |
| TaskLog | POST | /task/log | No | Submit task execution log | query: ; path: ; body: TaskExecutionLog | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| MaterialInfo | GET | /material/info/list | No | List materials (paged) | query: pageNum,pageSize,name,category; path: ; body: - | R<Page<MaterialInfo>> | Pagination response data fields: current, size, total, records. |
| MaterialInfo | POST | /material/info | No | Create material | query: ; path: ; body: MaterialInfo | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| MaterialInfo | PUT | /material/info | No | Update material | query: ; path: ; body: MaterialInfo | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| MaterialInfo | DELETE | /material/info/{ids} | No | Delete materials by CSV ids | query: ; path: ids; body: - | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| MaterialLog | GET | /material/log/list | No | List material inout logs (paged) | query: pageNum,pageSize,materialId; path: ; body: - | R<Page<MaterialInoutLog>> | Pagination response data fields: current, size, total, records. |
| MaterialLog | POST | /material/log/execute | No | Execute material in/out | query: ; path: ; body: MaterialInoutLog | R<Void> | Response follows R<T>; success code=200, failure code!=200. |
| Report | GET | /report/dashboard | No | Dashboard data | query: ; path: ; body: - | R<DashboardData> | Response follows R<T>; success code=200, failure code!=200. |
