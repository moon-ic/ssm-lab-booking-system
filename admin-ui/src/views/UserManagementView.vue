<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAdmin,
  createStudent,
  createTeacher,
  deleteStudent,
  getUserDetail,
  listUsers,
  resetPassword,
  updateUserStatus
} from '@/api/users'
import { useAuthStore } from '@/store/auth'
import type {
  CreateAdminPayload,
  CreateStudentPayload,
  CreateTeacherPayload,
  ResetPasswordPayload,
  UserListItem,
  UserListQuery,
  UserRoleCode,
  UserStatus
} from '@/types/user'

const authStore = useAuthStore()
const loading = ref(false)
const detailLoading = ref(false)
const createDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const createFormRef = ref()

const filters = reactive<UserListQuery>({
  keyword: '',
  roleCode: undefined,
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

const page = reactive({
  list: [] as UserListItem[],
  total: 0
})

const detail = ref<UserListItem | null>(null)

const createForm = reactive({
  name: '',
  account: '',
  phone: ''
})

const currentRole = computed(() => (authStore.state.currentUser?.roleCode ?? authStore.state.session?.userInfo.roleCode ?? 'STUDENT') as UserRoleCode)

const roleOptions = computed(() => {
  const base = [
    { label: '管理员', value: 'ADMIN' },
    { label: '教师', value: 'TEACHER' },
    { label: '学生', value: 'STUDENT' }
  ] as const

  switch (currentRole.value) {
    case 'SUPER_ADMIN':
      return [base[0]]
    case 'ADMIN':
      return [base[1]]
    case 'TEACHER':
      return [base[2]]
    default:
      return []
  }
})

const selectedCreateRole = ref<UserRoleCode>(roleOptions.value[0]?.value ?? 'STUDENT')

const filterRoleOptions = computed(() => {
  switch (currentRole.value) {
    case 'SUPER_ADMIN':
      return ['ADMIN', 'TEACHER', 'STUDENT'] as UserRoleCode[]
    case 'ADMIN':
      return ['TEACHER', 'STUDENT'] as UserRoleCode[]
    case 'TEACHER':
      return ['STUDENT'] as UserRoleCode[]
    default:
      return [] as UserRoleCode[]
  }
})

const createRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  account: [{ required: true, message: '请输入账号或编号', trigger: 'blur' }]
}

function createFieldLabel(roleCode: UserRoleCode) {
  if (roleCode === 'ADMIN') {
    return '账号'
  }
  if (roleCode === 'TEACHER') {
    return '工号'
  }
  return '学号'
}

function createRoleLabel(roleCode: UserRoleCode) {
  if (roleCode === 'SUPER_ADMIN') return '超级管理员'
  if (roleCode === 'ADMIN') return '管理员'
  if (roleCode === 'TEACHER') return '教师'
  return '学生'
}

function createStatusLabel(status: UserStatus) {
  return status === 'ENABLED' ? '启用' : '停用'
}

function resetCreateForm() {
  createForm.name = ''
  createForm.account = ''
  createForm.phone = ''
}

async function loadUsers() {
  loading.value = true

  try {
    const data = await listUsers(filters)
    page.list = data.list
    page.total = data.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  filters.pageNum = 1
  void loadUsers()
}

function handleReset() {
  filters.keyword = ''
  filters.roleCode = undefined
  filters.status = undefined
  filters.pageNum = 1
  void loadUsers()
}

async function openDetail(userId: number) {
  detailDialogVisible.value = true
  detailLoading.value = true

  try {
    detail.value = await getUserDetail(userId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

async function submitCreate() {
  await createFormRef.value?.validate()

  try {
    if (selectedCreateRole.value === 'ADMIN') {
      await createAdmin({
        name: createForm.name,
        account: createForm.account,
        phone: createForm.phone
      } satisfies CreateAdminPayload)
    } else if (selectedCreateRole.value === 'TEACHER') {
      await createTeacher({
        name: createForm.name,
        jobNo: createForm.account,
        phone: createForm.phone
      } satisfies CreateTeacherPayload)
    } else {
      await createStudent({
        name: createForm.name,
        studentNo: createForm.account,
        phone: createForm.phone
      } satisfies CreateStudentPayload)
    }

    ElMessage.success(`${createRoleLabel(selectedCreateRole.value)}创建成功，初始密码为 0000`)
    createDialogVisible.value = false
    resetCreateForm()
    await loadUsers()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建失败')
  }
}

async function handleStatusChange(row: UserListItem, status: UserStatus) {
  try {
    await updateUserStatus(row.userId, { status })
    ElMessage.success('用户状态已更新')
    await loadUsers()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '状态更新失败')
  }
}

async function handleResetPassword(row: UserListItem) {
  try {
    await ElMessageBox.confirm(
      `Reset ${row.name}'s password to 0000 and require password change on next login?`,
      '重置密码',
      {
        confirmButtonText: '确认重置',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await resetPassword(row.userId, { newPassword: '0000' } satisfies ResetPasswordPayload)
    ElMessage.success('密码重置成功')
    await loadUsers()
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : '密码重置失败')
  }
}

async function handleDeleteStudent(row: UserListItem) {
  try {
    await ElMessageBox.confirm(`确认删除 ${row.name} 吗？该操作仅适用于学生账号。`, '删除学生', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteStudent(row.userId)
    ElMessage.success('学生账号已删除')
    await loadUsers()
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function canManageStatus() {
  return currentRole.value === 'SUPER_ADMIN' || currentRole.value === 'ADMIN'
}

function canDeleteStudent(row: UserListItem) {
  return currentRole.value === 'TEACHER' && row.roleCode === 'STUDENT'
}

onMounted(() => {
  if (roleOptions.value.length > 0) {
    selectedCreateRole.value = roleOptions.value[0].value
  }
  void loadUsers()
})
</script>

<template>
  <div class="user-page">
    <section class="hero-card">
      <span class="eyebrow">用户模块</span>
      <h2>按角色管理可见账号</h2>
      <p>
        本模块遵循 PRD 的权限规则：超级管理员管理管理员，管理员管理教师，教师管理自己名下的学生。
      </p>
    </section>

    <section class="toolbar-card">
      <div class="filter-grid">
        <ElInput v-model="filters.keyword" placeholder="按姓名、账号或登录编号搜索" clearable @keyup.enter="handleSearch" />
        <ElSelect v-model="filters.roleCode" placeholder="角色" clearable>
          <ElOption v-for="role in filterRoleOptions" :key="role" :label="createRoleLabel(role)" :value="role" />
        </ElSelect>
        <ElSelect v-model="filters.status" placeholder="状态" clearable>
          <ElOption label="启用" value="ENABLED" />
          <ElOption label="停用" value="DISABLED" />
        </ElSelect>
      </div>

      <div class="toolbar-actions">
        <ElButton @click="handleReset">重置</ElButton>
        <ElButton type="primary" @click="handleSearch">查询</ElButton>
        <ElButton v-if="roleOptions.length" type="success" @click="createDialogVisible = true">新增用户</ElButton>
      </div>
    </section>

    <section class="table-card">
      <ElTable :data="page.list" v-loading="loading" width="100%">
        <ElTableColumn prop="name" label="姓名" min-width="140" />
        <ElTableColumn prop="account" label="账号" min-width="140" />
        <ElTableColumn prop="jobNoOrStudentNo" label="登录编号" min-width="140" />
        <ElTableColumn prop="roleCode" label="角色" min-width="120">
          <template #default="{ row }">
            <ElTag>{{ createRoleLabel(row.roleCode) }}</ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="status" label="状态" min-width="120">
          <template #default="{ row }">
            <ElTag :type="row.status === 'ENABLED' ? 'success' : 'info'">
              {{ createStatusLabel(row.status) }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="creditScore" label="信用分" min-width="100" />
        <ElTableColumn prop="firstLoginRequired" label="首次登录" min-width="120">
          <template #default="{ row }">
            <ElTag :type="row.firstLoginRequired ? 'warning' : 'success'">
              {{ row.firstLoginRequired ? '待修改' : '已完成' }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="操作" min-width="300" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <ElButton link type="primary" @click="openDetail(row.userId)">详情</ElButton>
              <ElButton
                v-if="canManageStatus()"
                link
                type="warning"
                @click="handleStatusChange(row, row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED')"
              >
                {{ row.status === 'ENABLED' ? '停用' : '启用' }}
              </ElButton>
              <ElButton v-if="canManageStatus()" link type="danger" @click="handleResetPassword(row)">重置密码</ElButton>
              <ElButton v-if="canDeleteStudent(row)" link type="danger" @click="handleDeleteStudent(row)">删除</ElButton>
            </div>
          </template>
        </ElTableColumn>
      </ElTable>

      <div class="pagination-wrap">
        <ElPagination
          v-model:current-page="filters.pageNum"
          v-model:page-size="filters.pageSize"
          background
          layout="total, prev, pager, next"
          :total="page.total"
          @current-change="loadUsers"
        />
      </div>
    </section>

    <ElDialog v-model="createDialogVisible" title="新增用户" width="460px" @closed="resetCreateForm">
      <ElForm ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <ElFormItem label="角色">
          <ElRadioGroup v-model="selectedCreateRole">
            <ElRadioButton v-for="role in roleOptions" :key="role.value" :label="role.value">
              {{ role.label }}
            </ElRadioButton>
          </ElRadioGroup>
        </ElFormItem>
        <ElFormItem label="姓名" prop="name">
          <ElInput v-model="createForm.name" placeholder="请输入姓名" />
        </ElFormItem>
        <ElFormItem :label="createFieldLabel(selectedCreateRole)" prop="account">
          <ElInput v-model="createForm.account" :placeholder="`请输入${createFieldLabel(selectedCreateRole)}`" />
        </ElFormItem>
        <ElFormItem label="手机号">
          <ElInput v-model="createForm.phone" placeholder="选填手机号" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="createDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="submitCreate">创建</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="detailDialogVisible" title="用户详情" width="520px">
      <ElSkeleton v-if="detailLoading" :rows="6" animated />
      <div v-else-if="detail" class="detail-grid">
        <article>
          <strong>姓名</strong>
          <span>{{ detail.name }}</span>
        </article>
        <article>
          <strong>账号</strong>
          <span>{{ detail.account }}</span>
        </article>
        <article>
          <strong>登录编号</strong>
          <span>{{ detail.jobNoOrStudentNo }}</span>
        </article>
        <article>
          <strong>角色</strong>
          <span>{{ createRoleLabel(detail.roleCode) }}</span>
        </article>
        <article>
          <strong>状态</strong>
          <span>{{ createStatusLabel(detail.status) }}</span>
        </article>
        <article>
          <strong>手机号</strong>
          <span>{{ detail.phone || '--' }}</span>
        </article>
        <article>
          <strong>信用分</strong>
          <span>{{ detail.creditScore }}</span>
        </article>
        <article>
          <strong>首次登录</strong>
          <span>{{ detail.firstLoginRequired ? '待修改密码' : '已完成' }}</span>
        </article>
      </div>
    </ElDialog>
  </div>
</template>

<style scoped>
.user-page {
  display: grid;
  gap: 20px;
}

.hero-card,
.toolbar-card,
.table-card {
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
  color: var(--theme-primary);
  background: var(--theme-primary-soft);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.toolbar-card {
  display: grid;
  gap: 16px;
}

.filter-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr 1fr;
  gap: 14px;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.detail-grid article {
  display: grid;
  gap: 6px;
  padding: 16px;
  border-radius: 18px;
  background: #f8fafc;
}

.detail-grid span {
  color: #475569;
}

@media (max-width: 960px) {
  .filter-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .toolbar-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>


