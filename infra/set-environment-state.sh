#!/usr/bin/env bash
set -euo pipefail

case "${1:-}" in
  on) enabled=true ;;
  off) enabled=false ;;
  *) echo "Use on ou off." >&2; exit 2 ;;
esac

: "${AWS_CFN_EXECUTION_ROLE_ARN:?Cadastre a variável AWS_CFN_EXECUTION_ROLE_ARN}"
stack=fiap-tc4-dev
database=dev-fiap-tc4-db

stack_json=$(aws cloudformation describe-stacks --stack-name "$stack" --output json)
stack_status=$(jq -r '.Stacks[0].StackStatus' <<< "$stack_json")
case "$stack_status" in
  CREATE_COMPLETE|UPDATE_COMPLETE|UPDATE_ROLLBACK_COMPLETE) ;;
  *) echo "A stack $stack precisa estar pronta. Estado atual: $stack_status." >&2; exit 1 ;;
esac

if ! jq -e '
  .Stacks[0].Parameters | map(.ParameterKey) |
  (index("EnableNatGateway") != null and index("ServicesEnabled") != null)
' <<< "$stack_json" > /dev/null; then
  echo "Publique o template com EnableNatGateway e ServicesEnabled antes de usar este workflow." >&2
  exit 1
fi

set_database_state() {
  local target=$1 status attempt
  for ((attempt = 0; attempt < 60; attempt++)); do
    status=$(aws rds describe-db-instances --db-instance-identifier "$database" \
      --query 'DBInstances[0].DBInstanceStatus' --output text)
    case "$target:$status" in
      available:available|stopped:stopped) return 0 ;;
      available:stopped) aws rds start-db-instance --db-instance-identifier "$database" > /dev/null ;;
      stopped:available) aws rds stop-db-instance --db-instance-identifier "$database" > /dev/null ;;
      *:starting|*:stopping|*:rebooting|*:backing-up|*:modifying|*:maintenance) ;;
      *) echo "Estado inesperado do RDS: $status." >&2; return 1 ;;
    esac
    sleep 30
  done
  echo "O RDS não chegou ao estado $target no tempo esperado." >&2
  return 1
}

if [[ $enabled == true ]]; then
  echo "Aguardando o RDS antes de reativar os serviços."
  set_database_state available
fi

if ! jq -e --arg enabled "$enabled" '
  .Stacks[0].Parameters |
  map(select(.ParameterKey == "EnableNatGateway" or .ParameterKey == "ServicesEnabled")) |
  all(.ParameterValue == $enabled)
' <<< "$stack_json" > /dev/null; then
  parameters=$(jq -c --arg enabled "$enabled" '
    .Stacks[0].Parameters | map(
      if .ParameterKey == "EnableNatGateway" or .ParameterKey == "ServicesEnabled"
      then {ParameterKey, ParameterValue: $enabled}
      else {ParameterKey, UsePreviousValue: true}
      end
    )
  ' <<< "$stack_json")

  change_set=$(aws cloudformation create-change-set --stack-name "$stack" \
    --change-set-name "environment-${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT}" \
    --change-set-type UPDATE --use-previous-template --capabilities CAPABILITY_NAMED_IAM \
    --role-arn "$AWS_CFN_EXECUTION_ROLE_ARN" --parameters "$parameters" --query Id --output text)
  if ! aws cloudformation wait change-set-create-complete --change-set-name "$change_set"; then
    aws cloudformation describe-change-set --change-set-name "$change_set" \
      --query '{Status:Status,Reason:StatusReason}' --output json
    aws cloudformation delete-change-set --change-set-name "$change_set"
    exit 1
  fi
  aws cloudformation execute-change-set --change-set-name "$change_set"
  aws cloudformation wait stack-update-complete --stack-name "$stack"
fi

if [[ $enabled == false ]]; then
  echo "Parando o RDS após pausar os serviços."
  set_database_state stopped
fi
