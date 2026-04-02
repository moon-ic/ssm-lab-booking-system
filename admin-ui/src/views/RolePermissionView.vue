<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  assignPermissions,
  createRole,
  getRoleDetail,
  listMenus,
  listPermissions,
  listRoles,
  updateRole
} from '@/api/roles'
import type { MenuItemOption, PermissionItem, RoleItem, SaveRolePayload } from '@/types/role'

const loading = ref(false)
const detailLoading = ref(false)
const saveDialogVisible = ref(false)
const assignDrawerVisible = ref(false)
const saveFormRef = ref()

const roles = ref<RoleItem[]>([])
const selectedRoleId = ref<number>()
const selectedRole = ref<RoleItem | null>(null)
const permissions = ref<PermissionItem[]>([])
const menus = ref<MenuItemOption[]>([])

const saveForm = reactive({
  roleId: undefined as number | undefined,
  roleName: '',
  roleCode: '',
  remark: ''
})

const assignmentForm = reactive({
  permissionIds: [] as number[],
  menuIds: [] as number[]
})

const saveRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

const actionPermissions = computed(() => permissions.value.filter((item) => item.type === 'ACTION'))
const menuPermissionCodes = computed(() => new Map(menus.value.map((item) => [item.menuId, item.permissionCode])))

async function loadInitialData() {
  loading.value = true

  try {
    const [roleList, permissionList, menuList] = await Promise.all([
      listRoles(),
      listPermissions(),
      listMenus()
    ])

    roles.value = roleList
    permissions.value = permissionList
    menus.value = menuList

    if (roleList.length > 0) {
      await selectRole(roleList[0].roleId)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载角色数据失败')
  } finally {
    loading.value = false
  }
}

async function selectRole(roleId: number) {
  selectedRoleId.value = roleId
  detailLoading.value = true

  try {
    const detail = await getRoleDetail(roleId)
    selectedRole.value = detail
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载角色详情失败')
  } finally {
    detailLoading.value = false
  }
}

function openCreateDialog() {
  saveForm.roleId = undefined
  saveForm.roleName = ''
  saveForm.roleCode = ''
  saveForm.remark = ''
  saveDialogVisible.value = true
}

function openEditDialog() {
  if (!selectedRole.value) {
    return
  }

  saveForm.roleId = selectedRole.value.roleId
  saveForm.roleName = selectedRole.value.roleName
  saveForm.roleCode = selectedRole.value.roleCode
  saveForm.remark = selectedRole.value.remark ?? ''
  saveDialogVisible.value = true
}

async function submitSave() {
  await saveFormRef.value?.validate()

  try {
    const payload = {
      roleName: saveForm.roleName,
      roleCode: saveForm.roleCode,
      remark: saveForm.remark
    } satisfies SaveRolePayload

    const role = saveForm.roleId
      ? await updateRole(saveForm.roleId, payload)
      : await createRole(payload)

    ElMessage.success(saveForm.roleId ? '角色更新成功' : '角色创建成功')
    saveDialogVisible.value = false
    await loadInitialData()
    await selectRole(role.roleId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  }
}

function openAssignDrawer() {
  if (!selectedRole.value) {
    return
  }

  assignmentForm.permissionIds = [...selectedRole.value.permissionIds]
  assignmentForm.menuIds = [...selectedRole.value.menuIds]
  assignDrawerVisible.value = true
}

async function submitAssignment() {
  if (!selectedRole.value) {
    return
  }

  try {
    const updated = await assignPermissions(selectedRole.value.roleId, {
      permissionIds: assignmentForm.permissionIds,
      menuIds: assignmentForm.menuIds
    })
    selectedRole.value = updated
    roles.value = roles.value.map((item) => (item.roleId === updated.roleId ? updated : item))
    assignDrawerVisible.value = false
    ElMessage.success('权限分配已更新')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '分配失败')
  }
}

function permissionName(permissionId: number) {
  return permissions.value.find((item) => item.permissionId === permissionId)?.permissionName ?? `#${permissionId}`
}

function menuName(menuId: number) {
  return menus.value.find((item) => item.menuId === menuId)?.menuName ?? `#${menuId}`
}

onMounted(() => {
  void loadInitialData()
})
</script>

<template>
  <div class="role-page">
    <section class="hero-card">
      <span class="eyebrow">角色模块</span>
      <h2>角色与权限管理</h2>
      <p>
        该页面面向超级管理员，支持创建角色、编辑元数据，并分配操作权限与可见菜单。
      </p>
    </section>

    <section class="workspace-grid" v-loading="loading">
      <article class="role-list-card">
        <div class="card-header">
          <div>
            <h3>角色列表</h3>
            <p>共 {{ roles.length }} 个角色</p>
          </div>
          <ElButton type="primary" @click="openCreateDialog">新建角色</ElButton>
        </div>

        <div class="role-list">
          <button
            v-for="role in roles"
            :key="role.roleId"
            type="button"
            class="role-item"
            :class="{ 'is-active': selectedRoleId === role.roleId }"
            @click="selectRole(role.roleId)"
          >
            <strong>{{ role.roleName }}</strong>
            <span>{{ role.roleCode }}</span>
            <small>{{ role.permissionIds.length }} 个权限，{{ role.menuIds.length }} 个菜单</small>
          </button>
        </div>
      </article>

      <article class="role-detail-card">
        <div class="card-header">
          <div>
            <h3>角色详情</h3>
            <p v-if="selectedRole">{{ selectedRole.roleCode }}</p>
          </div>
          <div class="header-actions">
            <ElButton :disabled="!selectedRole" @click="openEditDialog">编辑</ElButton>
            <ElButton type="success" :disabled="!selectedRole" @click="openAssignDrawer">分配权限</ElButton>
          </div>
        </div>

        <ElSkeleton v-if="detailLoading" :rows="8" animated />
        <template v-else-if="selectedRole">
          <div class="detail-meta">
            <article>
              <strong>角色名称</strong>
              <span>{{ selectedRole.roleName }}</span>
            </article>
            <article>
              <strong>角色编码</strong>
              <span>{{ selectedRole.roleCode }}</span>
            </article>
            <article>
              <strong>备注</strong>
              <span>{{ selectedRole.remark || '--' }}</span>
            </article>
          </div>

          <div class="chip-section">
            <h4>操作权限</h4>
            <div class="chip-list">
              <ElTag v-for="permissionId in selectedRole.permissionIds" :key="permissionId" type="primary">
                {{ permissionName(permissionId) }}
              </ElTag>
            </div>
          </div>

          <div class="chip-section">
            <h4>可见菜单</h4>
            <div class="chip-list">
              <ElTag v-for="menuId in selectedRole.menuIds" :key="menuId" type="success">
                {{ menuName(menuId) }}
              </ElTag>
            </div>
          </div>
        </template>
      </article>
    </section>

    <ElDialog v-model="saveDialogVisible" :title="saveForm.roleId ? '编辑角色' : '新建角色'" width="460px">
      <ElForm ref="saveFormRef" :model="saveForm" :rules="saveRules" label-position="top">
        <ElFormItem label="角色名称" prop="roleName">
          <ElInput v-model="saveForm.roleName" placeholder="请输入角色名称" />
        </ElFormItem>
        <ElFormItem label="角色编码" prop="roleCode">
          <ElInput v-model="saveForm.roleCode" placeholder="请输入角色编码" />
        </ElFormItem>
        <ElFormItem label="备注">
          <ElInput v-model="saveForm.remark" type="textarea" :rows="3" placeholder="选填备注" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="saveDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="submitSave">保存</ElButton>
      </template>
    </ElDialog>

    <ElDrawer v-model="assignDrawerVisible" title="分配权限" size="520px">
      <div class="assign-section">
        <h4>操作权限</h4>
        <ElCheckboxGroup v-model="assignmentForm.permissionIds" class="check-grid">
          <ElCheckbox v-for="permission in actionPermissions" :key="permission.permissionId" :value="permission.permissionId">
            {{ permission.permissionName }}
          </ElCheckbox>
        </ElCheckboxGroup>
      </div>

      <div class="assign-section">
        <h4>可见菜单</h4>
        <ElCheckboxGroup v-model="assignmentForm.menuIds" class="check-grid">
          <ElCheckbox v-for="menu in menus" :key="menu.menuId" :value="menu.menuId">
            {{ menu.menuName }} / {{ menu.path }}
            <span class="check-hint">{{ menuPermissionCodes.get(menu.menuId) }}</span>
          </ElCheckbox>
        </ElCheckboxGroup>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <ElButton @click="assignDrawerVisible = false">取消</ElButton>
          <ElButton type="primary" @click="submitAssignment">保存分配</ElButton>
        </div>
      </template>
    </ElDrawer>
  </div>
</template>

<style scoped>
.role-page {
  display: grid;
  gap: 20px;
}

.hero-card,
.role-list-card,
.role-detail-card {
  padding: 24px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.08);
}

.hero-card h2 {
  margin: 10px 0 12px;
  font-size: 30px;
}

.hero-card p {
  margin: 0;
  line-height: 1.7;
  color: #475569;
}

.eyebrow {
  display: inline-flex;
  width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  color: #155e75;
  background: #cffafe;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.workspace-grid {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.card-header h3 {
  margin: 0;
}

.card-header p {
  margin: 6px 0 0;
  color: #64748b;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.role-list {
  display: grid;
  gap: 12px;
}

.role-item {
  display: grid;
  gap: 6px;
  padding: 16px;
  border: 1px solid transparent;
  border-radius: 18px;
  text-align: left;
  cursor: pointer;
  background: #f8fafc;
}

.role-item.is-active {
  border-color: rgba(14, 116, 144, 0.28);
  background: #ecfeff;
}

.role-item span,
.role-item small {
  color: #64748b;
}

.detail-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.detail-meta article {
  display: grid;
  gap: 6px;
  padding: 16px;
  border-radius: 18px;
  background: #f8fafc;
}

.detail-meta span {
  color: #475569;
}

.chip-section {
  margin-top: 24px;
}

.chip-section h4 {
  margin: 0 0 12px;
}

.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.assign-section {
  display: grid;
  gap: 14px;
  margin-bottom: 24px;
}

.assign-section h4 {
  margin: 0;
}

.check-grid {
  display: grid;
  gap: 12px;
}

.check-hint {
  margin-left: 8px;
  color: #94a3b8;
  font-size: 12px;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 980px) {
  .workspace-grid,
  .detail-meta {
    grid-template-columns: 1fr;
  }

  .card-header,
  .header-actions {
    flex-direction: column;
  }
}
</style>
